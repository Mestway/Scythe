package symbolic;

import enumerator.context.EnumContext;
import enumerator.primitive.EnumCanonicalFilters;
import global.Statistics;
import mapping.CoordInstMap;
import mapping.MappingInference;
import sql.lang.Table;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
import util.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 */
public class CompoundSummaryTable extends AbstractSummaryTable {

    public AbstractSummaryTable st1, st2;
    // the set of filters that use both st1 elements and st2 elements,
    // original filters on st1 and st2 are not contained here
    Set<BVFilter> filtersLR = new HashSet<>();
    Map<BVFilter, Pair<Double, List<Filter>>> decodedLR = new HashMap<>();

    // this list will be ready after primitiveFiltersEvaluated is evaluate to true.
    List<BVFilter> primitives = new ArrayList<>();

    private boolean primitiveFiltersEvaluated = false;
    // the representitive table will not be available until primitives are evaluated
    // this tableNode represent the tablenode used in evaluating the filters
    RenameTableNode representitiveTableNode = null;

    public CompoundSummaryTable(AbstractSummaryTable st1, AbstractSummaryTable st2) {
        this.st1 = st1;
        this.st2 = st2;
        this.filtersLR = new HashSet<>();
    }

    @Override
    public Table getBaseTable() {
        return Table.joinTwo(st1.getBaseTable(), st2.getBaseTable());
    }

    @Override
    public List<Table> getAllPrimitiveBaseTables() {
        List<Table> result = st1.getAllPrimitiveBaseTables();
        result.addAll(st2.getAllPrimitiveBaseTables());
        return result;
    }

    // an version 2 that visits the tables with multi-level demotion
    @Override
    public void emitFinalVisitAllTables(
            MappingInference mi,
            EnumContext ec,
            BiConsumer<AbstractSummaryTable, BVFilter> f) {
        this.evalPrimitive(ec);

        Set<BVFilter> dummyExtFilter = new HashSet<>();
        dummyExtFilter.add(BVFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter()));

        Set<Pair<BVFilter, BVFilter>> r = this.visitDemotedSpace(ec, dummyExtFilter);

        for (Pair<BVFilter, BVFilter> p : r) {
            f.accept(this, p.getKey());
        }
    }

    // This method is actually very general, as we can also treat none-demoted visiting as a visit with only one id ext filter.
    // The result is a set of pairs p = (sf, ef),
    //  where sf is the a filter generated from combining f1*f2*lrf and ef is a filter from demotedExtFilters
    //  p is the pair that satisfies the property sf*ef is a filter that can generate output
    public Set<Pair<BVFilter, BVFilter>> visitDemotedSpace(
            EnumContext ec, Set<BVFilter> demotedExtFilters) {

        Set<Pair<BVFilter, BVFilter>> resultPairs = new HashSet<>();

        MappingInference mi = MappingInference.buildMapping(this.getBaseTable(), ec.getOutputTable());
        Set<BVFilter> targetFilters = new HashSet<>();

        // construct the target filters backwardly
        List<CoordInstMap> map = mi.genMappingInstancesWColumnBarrier(this.getTableRightIndexBoundries());

        //System.out.println("[CrossTableMapping Instances] " + map.size());
        for (CoordInstMap cim : map) {
            BVFilter sf = new BVFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        Set<BVFilter> st1FiltersLinks = st1.instantiateAllFilters();
        Set<BVFilter> st2FiltersLinks = st2.instantiateAllFilters();
        Set<BVFilter> lrFiltersLinks =
                AbstractSummaryTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));

        // map each filter in the sub-symbolic table to its promoted form in the cartesian product table.
        Map<BVFilter, BVFilter> promotedFilters1 = new HashMap<>();
        Map<BVFilter, BVFilter> promotedFilters2 = new HashMap<>();
        for (BVFilter f1 : st1FiltersLinks) {
            BVFilter promotedf1 = this.promoteLeftFilter(f1);
            if (fullyContainedAnElement(promotedf1, targetFilters))
                promotedFilters1.put(f1, promotedf1);
        }

        for (BVFilter f2 : st2FiltersLinks) {
            BVFilter promotedf2 = this.promoteRightFilter(f2);
            if (fullyContainedAnElement(promotedf2, targetFilters))
                promotedFilters2.put(f2, promotedf2);
        }

        Set<BVFilter> validLRFilters = new HashSet<>();
        for (BVFilter sf : lrFiltersLinks) {
            if (fullyContainedAnElement(sf, targetFilters))
                validLRFilters.add(sf);
        }

        // In the first part, we are trying to visit filters to infer filters that can yield to a cross table filtering
        for (BVFilter lrf : validLRFilters) {
            for (BVFilter f2 : promotedFilters2.keySet()) {
                BVFilter mergedlrf2 = BVFilter.mergeFilter(
                        lrf, promotedFilters2.get(f2), AbstractSummaryTable.mergeFunction);
                if (! fullyContainedAnElement(mergedlrf2, targetFilters))
                    continue;

                for (BVFilter ef : demotedExtFilters) {
                    BVFilter mergedlrf2ef = BVFilter.mergeFilter(
                            mergedlrf2, ef,
                            AbstractSummaryTable.mergeFunction
                    );

                    if (! fullyContainedAnElement(mergedlrf2ef, targetFilters))
                        continue;

                    for (BVFilter f1 : promotedFilters1.keySet()) {
                        BVFilter mergedlrf2eff1 = BVFilter.mergeFilter(
                                mergedlrf2ef, promotedFilters1.get(f1),
                                AbstractSummaryTable.mergeFunction
                        );
                        // Add the pair to the result set.
                        if (! targetFilters.contains(mergedlrf2eff1))
                            continue;

                        resultPairs.add(new Pair<>(
                                BVFilter.mergeFilter(mergedlrf2, promotedFilters1.get(f1), AbstractSummaryTable.mergeFunction),
                                ef));
                    }
                }
            }
        }

        MappingInference st1Mi = MappingInference.buildMapping(this.st1.getBaseTable(), ec.getOutputTable());
        boolean furtherDemotionToSt1 = st1Mi.isValidMapping();

        // If we cannot perform further demotion
        if (! furtherDemotionToSt1) return resultPairs;

        Set<BVFilter> st1TargetFilters = new HashSet<>();
        List<CoordInstMap> st1Map = st1Mi.genMappingInstances();
        //System.out.println("[demotedFilter Mapping Instances] " + st1Map.size());

        for (CoordInstMap cim : st1Map) {
            BVFilter sf = new BVFilter(cim.rowsInvolved(), st1.getBaseTable().getContent().size());
            st1TargetFilters.add(sf);
        }

        // map each filter in the sub-symbolic table to its promoted form in the cartesian product table.
        Map<BVFilter, BVFilter> validPromotedDemotedf2 = new HashMap<>();
        for (BVFilter f2 : st2FiltersLinks) {
            BVFilter promotedf2 = this.promoteRightFilter(f2);
            BVFilter lDemotedf2 = this.demoteToLeftFilter(promotedf2);
            if (fullyContainedAnElement(lDemotedf2, st1TargetFilters))
                validPromotedDemotedf2.put(f2, promotedf2);
        }
        Map<BVFilter, BVFilter> validLRDemoted = new HashMap<>();
        for (BVFilter sf : lrFiltersLinks) {
            BVFilter lDemotedLRF = this.demoteToLeftFilter(sf);
            if (fullyContainedAnElement(lDemotedLRF, st1TargetFilters))
                validLRDemoted.put(sf, lDemotedLRF);
        }
        Map<BVFilter, BVFilter> validExtDemoted = new HashMap<>();
        for (BVFilter ef : demotedExtFilters) {
            BVFilter lDemotedEF = this.demoteToLeftFilter(ef);
            if (fullyContainedAnElement(lDemotedEF, st1TargetFilters))
                validExtDemoted.put(ef, lDemotedEF);
        }

        // TODO: think carefully whether we should store all these mappings
        Map<BVFilter, Set<Pair<BVFilter, BVFilter>>> demotedCompoundToOriginalFilter = new HashMap<>();

        for (BVFilter lrf : validLRDemoted.keySet()) {
            for (BVFilter f2 : validPromotedDemotedf2.keySet()) {
                BVFilter mergedlrf2 = BVFilter.mergeFilter(
                        lrf, validPromotedDemotedf2.get(f2), AbstractSummaryTable.mergeFunction);
                if (! fullyContainedAnElement(demoteToLeftFilter(mergedlrf2), st1TargetFilters))
                    continue;
                for (BVFilter ef : validExtDemoted.keySet()) {
                    BVFilter mergedlrf2ef = BVFilter.mergeFilter(
                            mergedlrf2, ef,
                            AbstractSummaryTable.mergeFunction
                    );

                    BVFilter lDemotedMergedlrf2ef = demoteToLeftFilter(mergedlrf2ef);
                    if (! fullyContainedAnElement(lDemotedMergedlrf2ef, st1TargetFilters))
                        continue;

                    Pair<BVFilter, BVFilter> p = new Pair<>(mergedlrf2, ef);

                    if (! demotedCompoundToOriginalFilter.containsKey(lDemotedMergedlrf2ef)) {
                        demotedCompoundToOriginalFilter.put(lDemotedMergedlrf2ef, new HashSet<>());
                    }
                    demotedCompoundToOriginalFilter.get(lDemotedMergedlrf2ef).add(p);
                }
            }
        }

        Set<Pair<BVFilter, BVFilter>> demotedEvalResult = st1.visitDemotedSpace(ec, demotedCompoundToOriginalFilter.keySet());

        // the first element of p is a filter from st1, second element is a demoted filter
        for (Pair<BVFilter, BVFilter> p : demotedEvalResult) {
            BVFilter promotedF1 = this.promoteLeftFilter(p.getKey());

            // key of p2 is mergedf2lr, value of p2 is a extFilter
            for (Pair<BVFilter, BVFilter> p2 : demotedCompoundToOriginalFilter.get(p.getValue())) {
                BVFilter mergedf1f2lr = BVFilter.mergeFilter(promotedF1, p2.getKey(),
                        AbstractSummaryTable.mergeFunction);

                resultPairs.add(new Pair<>(mergedf1f2lr, p2.getValue()));
            }
        }

        return resultPairs;
    }

    @Override
    public Set<BVFilter> instantiateAllFilters() {

        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        //Map<SymbolicFilter, Integer> hmp = new HashMap<>();
        Set<BVFilter> instantiatedFilters = new HashSet<>();

        Set<BVFilter> st1FiltersLinks = st1.instantiateAllFilters();
        Set<BVFilter> st2FiltersLinks = st2.instantiateAllFilters();

        Set<BVFilter> lrFiltersLinks =
                AbstractSummaryTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));
        // if we want more than 1 level
        // lrFiltersLinks = AbstractSymbolicTable.mergeAndLinkFilters(this, lrFiltersLinks.getKey().stream().collect(Collectors.toList()));

        Map<BVFilter, BVFilter> promotedFilters1 = new HashMap<>();
        Map<BVFilter, BVFilter> promotedFilters2 = new HashMap<>();

        for (BVFilter f1 : st1FiltersLinks) {
            promotedFilters1.put(f1, this.promoteLeftFilter(f1));
        }
        for (BVFilter f2 : st2FiltersLinks) {
            promotedFilters2.put(f2, this.promoteRightFilter(f2));
        }

        int filtersToBeVisited = promotedFilters1.keySet().size() * promotedFilters2.keySet().size() * lrFiltersLinks.size();
        // System.out.println("Filters to be visited: " + filtersToBeVisited);

        Statistics.compound_case_cnt ++;
        Statistics.sum_compound_filter_cnt += filtersToBeVisited;
        Statistics.max_compound_filter_cnt = Statistics.max_compound_filter_cnt > filtersToBeVisited ? Statistics.max_compound_filter_cnt : filtersToBeVisited;

        int tt = 0;
        for (BVFilter f1 : st1FiltersLinks) {
            for (BVFilter f2 : st2FiltersLinks) {

                BVFilter mergedf1f2 = BVFilter.mergeFilter(
                        promotedFilters1.get(f1),
                        promotedFilters2.get(f2),
                        AbstractSummaryTable.mergeFunction);

                for (BVFilter lrf : lrFiltersLinks) {

                    tt ++;

                    BVFilter mergedFilter = BVFilter
                            .mergeFilter(mergedf1f2, lrf, AbstractSummaryTable.mergeFunction);

                    instantiatedFilters.add(mergedFilter);

                    if (tt % 100000 == 1000)
                        System.out.println("stored cnt / visited compound cnt " + instantiatedFilters.size() + " / " + tt + "(" + filtersToBeVisited + ")" + " = " + instantiatedFilters.size() / ((float) tt));

                    Set<Pair<AbstractSummaryTable, BVFilter>> src = new HashSet<>();
                    src.add(new Pair<>(st1, f1));
                    src.add(new Pair<>(st2, f2));
                    src.add(new Pair<>(this, lrf));
                }
            }
        }

        // System.out.println("#(BitVec)/#(CompoundFilter): " + instantiatedFilters.size() + " / " + tt + " = " + (((float)instantiatedFilters.size()) / tt));
        Statistics.sum_red_compound_to_bv += (tt / ((float)instantiatedFilters.size()));

        this.allfiltersEnumerated = true;
        this.totalBitVecFiltersCount = instantiatedFilters.size();
        return instantiatedFilters;
    }

    @Override
    public Optional<BVFilter> lazyFilterEval(Integer index) {

        assert primitiveFiltersEvaluated;

        int n3 = this.compoundPrimitiveFilterCount();
        int n2 = this.st2.compoundFilterCount();
        int n1 = this.st1.compoundFilterCount();

        if (index >= n1 * n2 * n3)
            return Optional.empty();

        int k = index % n3;
        int j = ((index - k) / n3) % n2;
        int i = (index - j * n3 - k) / n2 / n3;

        // (i) * n2 * n3 + (j) * n3 + (k)

        BVFilter p1 = st1.lazyFilterEval(i).get();
        BVFilter p2  = st2.lazyFilterEval(j).get();

        // the following code are used to generate a LR filter

        BVFilter LRFilter = null;

        if (k == 0) {
            LRFilter = BVFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter());
        } else {
            k --;
            Pair<Integer, Integer> xy = this.inverseFilterIndex(this.primitives.size(), k);
            LRFilter = BVFilter.mergeFilter(
                    primitives.get(xy.getKey()),
                    primitives.get(xy.getValue()),
                    AbstractSummaryTable.mergeFunction);

            // add the link from two source filter to the merged filter itself
            Set<Pair<AbstractSummaryTable, BVFilter>> srcs = new HashSet<>();
            srcs.add(new Pair<>(this, primitives.get(xy.getKey())));
            srcs.add(new Pair<>(this, primitives.get(xy.getValue())));
        }

        BVFilter mergedFilter = BVFilter.mergeFilter(
                this.promoteLeftFilter(p1),
                BVFilter.mergeFilter(
                        this.promoteRightFilter(p2),
                        LRFilter,
                        AbstractSummaryTable.mergeFunction),
                AbstractSummaryTable.mergeFunction);

        Set<Pair<AbstractSummaryTable, BVFilter>> src = new HashSet<>();
        src.add(new Pair<>(st1, p1));
        src.add(new Pair<>(st2, p2));
        src.add(new Pair<>(this, LRFilter));

        return Optional.of(mergedFilter);
    }

    @Override
    public int getPrimitiveFilterNum() {
        return this.filtersLR.size();
    }

    @Override
    void evalPrimitive(EnumContext ec) {
        if (this.primitiveFiltersEvaluated) return;

        // first evaluate the filters for sub-abstract tables
        st1.evalPrimitive(ec);
        st2.evalPrimitive(ec);

        JoinNode jn = new JoinNode(
                Arrays.asList(
                        new NamedTable(this.st1.getBaseTable()),
                        new NamedTable(this.st2.getBaseTable())));

        RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
        this.representitiveTableNode = rt;

        // the maximum filter length for filtersLR should be 1
        int backUpMaxFilterLength = ec.getMaxFilterLength();
        ec.setMaxFilterLength(1);
        List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterJoinNode(rt, ec);
        ec.setMaxFilterLength(backUpMaxFilterLength);

        // make sure that the empty filter is added
        filters.add(new EmptyFilter());
        decodedLR.put(BVFilter.genSymbolicFilterFromTableNode(rt, new EmptyFilter()), new Pair<>(0., new ArrayList<>()));

        for (Filter f : filters) {
            BVFilter symFilter = BVFilter.genSymbolicFilterFromTableNode(rt, f);

            this.filtersLR.add(symFilter);
            if (! decodedLR.containsKey(symFilter)) {
                decodedLR.put(symFilter, new Pair<>(99999., new ArrayList<>()));
            }

            Pair<Double, List<Filter>> entryRef = decodedLR.get(symFilter);

            entryRef.getValue().add(f);
            double cost = CostEstimator.estimateFilterCost(f,
                    TableNode.nameToOriginMap(
                            representitiveTableNode.getSchema(),
                            representitiveTableNode.originalColumnName()));
            if (cost < entryRef.getKey()) {
                decodedLR.put(symFilter, new Pair<>(cost, entryRef.getValue()));
            }
        }

        this.primitiveFiltersEvaluated = true;
        this.primitiveBitVecFilterCount = this.filtersLR.size();
        this.primitiveSynFilterCount = filters.size();

        // calculating the reduction rate from bit vector to syntax filters
        // System.out.println("#(BitVec)/#(SynFilter): " + this.filtersLR.size() + " / " + filters.size() + " = " + (((float)filtersLR.size()) / filters.size()));
        Statistics.red_syn_to_bv_case_cnt ++;
        Statistics.sum_red_syn_to_bv += ((float) filters.size()) / ((float)filtersLR.size());
    }


    // Promote a filter in left table to a filter for current baseTable
    private BVFilter promoteLeftFilter(BVFilter sf1) {
        Set<Integer> promotedFilter = new HashSet<>();
        for (int i : sf1.getFilterRep()) {
            for (int j  = 0; j < this.st2.getBaseTable().getContent().size(); j ++) {
                promotedFilter.add(i * this.st2.getBaseTable().getContent().size() + j);
            }
        }
        return new BVFilter(promotedFilter, this.getBaseTable().getContent().size());
    }

    // Promote a filter in right table to a filter for current baseTable
    private BVFilter promoteRightFilter(BVFilter sf2) {
        Set<Integer> promotedFilter = new HashSet<>();
        for (int i = 0; i < st1.getBaseTable().getContent().size(); i ++) {
            for (int j : sf2.getFilterRep()) {
                promotedFilter.add(i * this.st2.getBaseTable().getContent().size() + j);
            }
        }
        return new BVFilter(promotedFilter, this.getBaseTable().getContent().size());
    }

    private BVFilter demoteToLeftFilter(BVFilter f) {
        Set<Integer> demotedFilter = new HashSet<>();

        for (Integer n : f.getFilterRep()) {
            demotedFilter.add(n / st2.getBaseTable().getContent().size());
        }

        /* for (int i = 0; i < st1.getBaseTable().getContent().size(); i ++) {
            for (int j = 0; j < st2.getBaseTable().getContent().size(); j ++) {
                if (f.getFilterRep().contains(i * st2.getBaseTable().getContent().size() + j)) {
                    demotedFilter.add(i);
                }
            }
        } */
        return new BVFilter(demotedFilter, st1.getBaseTable().getContent().size());
    }

    private BVFilter demoteToRightFilter(BVFilter f) {
        Set<Integer> demotedFilter = new HashSet<>();

        for (Integer n : f.getFilterRep()) {
            demotedFilter.add(n % st2.getBaseTable().getContent().size());
        }
        return new BVFilter(demotedFilter, st2.getBaseTable().getContent().size());
    }


    public List<Filter> decodeLR(BVFilter sf) {
        return decodedLR.get(sf).getValue();
    }

    @Override
    public TableNode queryForBaseTable(EnumContext ec) {
        return RenameTNWrapper.tryRename(new JoinNode(Arrays.asList(st1.queryForBaseTable(ec), st2.queryForBaseTable(ec))));
    }

    @Override
    public int compoundPrimitiveFilterCount() {
        return this.getPrimitiveFilterNum() * (this.getPrimitiveFilterNum() - 1) / 2 + 1;
    }

    @Override
    public int compoundFilterCount() {
        int n3 = this.compoundPrimitiveFilterCount();
        int n2 = this.st2.compoundFilterCount();
        int n1 = this.st1.compoundFilterCount();
        return n3 * n2 * n1;
    }

    @Override
    public List<Integer> getTableRightIndexBoundries() {
        List<Integer> result = new ArrayList<>();
        List<Integer> leftBoundary = st1.getTableRightIndexBoundries();
        List<Integer> rightBoundary = st2.getTableRightIndexBoundries()
                .stream().map(x -> x + st1.getBaseTable().getMetadata().size()).collect(Collectors.toList());
        result.addAll(leftBoundary);
        result.addAll(rightBoundary);
        return result;
    }

    @Override
    public Map<BVFilter, List<BVFilterCompTree>> batchGenDecomposition(Set<BVFilter> targets) {

        Set<BVFilter> st1FiltersLinks = st1.instantiateAllFilters();
        Set<BVFilter> st2FiltersLinks = st2.instantiateAllFilters();
        Set<BVFilter> lrFiltersLinks =
                AbstractSummaryTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));

        Map<BVFilter, BVFilter> promotedFilters1 = new HashMap<>();
        Map<BVFilter, BVFilter> promotedFilters2 = new HashMap<>();

        for (BVFilter f1 : st1FiltersLinks) {
            BVFilter promotedf1 = this.promoteLeftFilter(f1);
            if (fullyContainedAnElement(promotedf1, targets))
                promotedFilters1.put(f1, promotedf1);
        }
        for (BVFilter f2 : st2FiltersLinks) {
            BVFilter promotedf2 = this.promoteRightFilter(f2);
            if (fullyContainedAnElement(promotedf2, targets))
                promotedFilters2.put(f2, promotedf2);
        }

        Set<BVFilter> validLRFilters = new HashSet<>();
        for (BVFilter sf : lrFiltersLinks) {
            if (fullyContainedAnElement(sf, targets))
                validLRFilters.add(sf);
        }

        // Set<Triple<SymbolicFilter, SymbolicFilter, SymbolicFilter>> is the components that compose
        // together will generate the symbolic filter that stored in the key, the tree elements in a triple are lrf, f1, f2,
        // respectively.
        // (since a key can have many different combination ways, we use a set to store it)
        Map<BVFilter, Set<Triple<BVFilter, BVFilter, BVFilter>>> resultToProcess = new HashMap<>();

        for (BVFilter sf : targets) {
            resultToProcess.put(sf, new HashSet<>());
        }

        for (BVFilter f1 : promotedFilters1.keySet()) {
            for (BVFilter f2 : promotedFilters2.keySet()) {

                BVFilter mergef1f2 = BVFilter.mergeFilter(
                        promotedFilters1.get(f1), promotedFilters2.get(f2), AbstractSummaryTable.mergeFunction);

                if (! fullyContainedAnElement(mergef1f2, targets))
                    continue;

                for (BVFilter lrf : validLRFilters) {

                    BVFilter mergef1f2lr = BVFilter.mergeFilter(
                            mergef1f2, lrf, AbstractSummaryTable.mergeFunction);

                    if (targets.contains(mergef1f2lr)) {
                        Triple<BVFilter, BVFilter, BVFilter> triple = new Triple<>(lrf, f1, f2);
                        resultToProcess.get(mergef1f2lr).add(triple);
                    }
                }
            }
        }

        // For doulecheck and debugging purpose
        if (resultToProcess.values().stream().map(x -> x.isEmpty()).reduce(false, (x, y)->(x || y)))
            System.err.println("[FATAL ERROR][SymbolicCompoundTable 707] exists filters that cannot be decomposed.");

        Map<BVFilter, Set<Set<BVFilter>>> decomposedLR = new HashMap<>();
        Set<BVFilter> st1FiltersToDecode = new HashSet<>();
        Set<BVFilter> st2FiltersToDecode = new HashSet<>();

        for (Map.Entry<BVFilter, Set<Triple<BVFilter, BVFilter, BVFilter>>>
                e : resultToProcess.entrySet()) {
            for (Triple<BVFilter, BVFilter, BVFilter> triple : e.getValue()) {
                decomposedLR.put(triple.first(), new HashSet<>());
                st1FiltersToDecode.add(triple.second());
                st2FiltersToDecode.add(triple.third());
            }
        }

        Map<BVFilter, List<BVFilterCompTree>> st1Trees = st1.batchGenDecomposition(st1FiltersToDecode);
        Map<BVFilter, List<BVFilterCompTree>> st2Trees = st2.batchGenDecomposition(st2FiltersToDecode);

        // The decoding result for each compound lr filter
        List<BVFilter> usefulPrimitive = this.filtersLR.stream()
                .filter(f -> fullyContainedAnElement(f, decomposedLR.keySet()))
                .collect(Collectors.toList());

        for (int i = 0; i < usefulPrimitive.size(); i ++) {
            for (int j = i + 1; j < usefulPrimitive.size(); j ++) {
                BVFilter mergedij = BVFilter.mergeFilter(usefulPrimitive.get(i),
                        usefulPrimitive.get(j), AbstractSummaryTable.mergeFunction);
                if (decomposedLR.containsKey(mergedij)) {
                    Set<BVFilter> oneDecomposition = new HashSet<>();

                    oneDecomposition.add(usefulPrimitive.get(i));
                    oneDecomposition.add(usefulPrimitive.get(j));
                    decomposedLR.get(mergedij).add(oneDecomposition);
                }
            }
        }

        BVFilter emptyFilter = BVFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter());
        if (decomposedLR.containsKey(emptyFilter)) {
            Set<BVFilter> s = new HashSet<>();
            s.add(emptyFilter);
            decomposedLR.get(emptyFilter).add(s);
        }

        if (decomposedLR.values().stream().map(x -> x.isEmpty()).reduce(false, (x, y)->(x || y)))
            System.err.println("[FATAL ERROER][SymbolicCompoundTable 707] exists filters that cannot be decomposed.");

        Map<BVFilter, List<BVFilterCompTree>> result = new HashMap<>();
        for (BVFilter t : targets) {
            result.put(t, new ArrayList<>());
        }

        for (Map.Entry<BVFilter, Set<Triple<BVFilter, BVFilter, BVFilter>>>
                e : resultToProcess.entrySet()) {
            for (Triple<BVFilter, BVFilter, BVFilter> triple : e.getValue()) {
                for (BVFilterCompTree st1SubTree : st1Trees.get(triple.second())) {
                    for (BVFilterCompTree st2SubTree :  st2Trees.get(triple.third())) {
                        for (Set<BVFilter> lrDecomposition : decomposedLR.get(triple.first())) {

                            BVFilterCompTree sfct = new BVFilterCompTree(this, lrDecomposition);
                            sfct.addChildren(st1SubTree);
                            sfct.addChildren(st2SubTree);

                            result.get(e.getKey()).add(sfct);
                        }
                    }
                }
            }
        }

        if (resultToProcess.values().stream().map(x -> x.isEmpty()).reduce(false, (x, y)->(x || y)))
            System.err.println("[FATAL ERROR][SymbolicCompoundTable 707] exists filters that cannot be decomposed.");

        Map<BVFilter, List<BVFilterCompTree>> postProcessed = new HashMap<>();

        // limit the number of trees generated from one single source, sort by their score
        for (Map.Entry<BVFilter, List<BVFilterCompTree>> i : result.entrySet()) {
            List<BVFilterCompTree> trees = i.getValue();

            Statistics.cntDecomposeTreeCount ++;
            Statistics.sumDecomposeTreeCount += trees.size();
            Statistics.maxDecomposeTreeCount = Statistics.maxDecomposeTreeCount > trees.size()? Statistics.maxDecomposeTreeCount : trees.size();

            if (trees.size() > 5) {
                trees.sort(new Comparator<BVFilterCompTree>() {
                    @Override
                    public int compare(BVFilterCompTree o1, BVFilterCompTree o2) {
                        double cost1 = o1.estimateTreeCost();
                        double cost2 = o2.estimateTreeCost();
                        return cost1 <= cost2 ?  (cost1 < cost2 ? -1 : 0) : 1;
                    }
                });
                //TODO: if we want to limit the number, use the commented one
                trees = trees.subList(0, 5);
                postProcessed.put(i.getKey(), trees);
            } else {
                postProcessed.put(i.getKey(), i.getValue());
            }
        }

        return postProcessed;
    }

    @Override
    public double estimatePrimitiveSymFilterCost(BVFilter sf) {
        return this.decodedLR.get(sf).getKey();
    }

}
