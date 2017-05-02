package summarytable;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.FilterEnumerator;
import global.GlobalConfig;
import global.Statistics;
import backward_inference.MappingInference;
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

    public enum CompositionType { join, union }

    AbstractSummaryTable st1, st2;
    CompositionType compositionType;

    // the set of filters that use both st1 elements and st2 elements,
    // original filters on st1 and st2 are not contained here
    Set<BVFilter> filtersLR = new HashSet<>();
    // map bv filters to their concrete forms, the Double field represents the minimum cost of the filter
    Map<BVFilter, Pair<Double, List<Filter>>> decodedLR = new HashMap<>();

    private boolean primitiveFiltersEvaluated = false;
    // the representative table will not be available until primitives are evaluated
    // this tableNode represent the table node used in evaluating the filters
    RenameTableNode representativeTableNode = null;

    public CompoundSummaryTable(AbstractSummaryTable st1, AbstractSummaryTable st2, CompositionType type) {
        this.st1 = st1;
        this.st2 = st2;
        this.compositionType = type;
        this.filtersLR = new HashSet<>();
    }

    @Override
    public Table getBaseTable() {
        if (compositionType.equals(CompositionType.join))
            return Table.joinTwo(st1.getBaseTable(), st2.getBaseTable());
        else if (compositionType.equals(CompositionType.union))
            return Table.union(st1.getBaseTable(), st2.getBaseTable());
        else
            return null;
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

        this.encodePrimitiveFilters(ec);
        bvSynthesisWithRangeMatching(mi, f);
    }

    /**
     * Given a mapping inference data structure, instantiate all valid filters and evaluate them on
     * @param mi an inferred MappingInference mi
     * @param f a consumer that consumes all tables after synthesis
     */
    public void bvSynthesisWithRangeMatching(
            MappingInference mi,
            BiConsumer<AbstractSummaryTable, BVFilter> f ) {

        List<List<Set<Integer>>> rowMappingRangeInstances =
                mi.genConcreteColMappings()
                        .stream()
                        .map(l -> mi.genRowMappingRange(l))
                        .collect(Collectors.toList());

        Set<BVFilter> st1Filters = st1.instantiateAllFilters();
        Set<BVFilter> st2Filters = st2.instantiateAllFilters();
        Set<BVFilter> lrFilters = AbstractSummaryTable.genConjunctiveFilters(this,
                this.filtersLR.stream().collect(Collectors.toList()));

        Map<BVFilter, BVFilter> promotedFilters1 = new HashMap<>();
        Map<BVFilter, BVFilter> promotedFilters2 = new HashMap<>();

        for (BVFilter f1 : st1Filters) {
            BVFilter promotedf1 = this.pullUpLeftFilter(f1);
            if (fullyContainedARange(promotedf1, rowMappingRangeInstances))
                promotedFilters1.put(f1, promotedf1);
        }
        for (BVFilter f2 : st2Filters) {
            BVFilter promotedf2 = this.pullUpRightFilter(f2);
            if (fullyContainedARange(promotedf2, rowMappingRangeInstances))
                promotedFilters2.put(f2, promotedf2);
        }

        Set<BVFilter> validLRFilters = new HashSet<>();
        for (BVFilter sf : lrFilters) {
            if (fullyContainedARange(sf, rowMappingRangeInstances))
                validLRFilters.add(sf);
        }

        int STATSvisitedCount = 0;

        for (BVFilter f1 : promotedFilters1.keySet()) {
            for (BVFilter f2 : promotedFilters2.keySet()) {

                BVFilter mergef1f2 = BVFilter.mergeFilterConj(
                        promotedFilters1.get(f1), promotedFilters2.get(f2));

                if (! fullyContainedARange(mergef1f2, rowMappingRangeInstances))
                    continue;

                for (BVFilter lrf : validLRFilters) {

                    STATSvisitedCount ++;

                    BVFilter mergef1f2lr = BVFilter.mergeFilterConj(
                            mergef1f2, lrf);

                    if (rowMappingRangeInstances
                            .stream()
                            .map(ls -> mergef1f2lr.exactMatchRange(ls))
                            .reduce((x,y)->x || y).get()) {
                        f.accept(this, mergef1f2lr);
                    }
                }
            }
        }

        if (GlobalConfig.STAT_MODE) {
            System.out.println("[Backward Prune Effectiveness] "
                    + ((double) st1Filters.size() * st2Filters.size() * lrFilters.size()) / STATSvisitedCount);
        }
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
                AbstractSummaryTable.genConjunctiveFilters(this, this.filtersLR.stream().collect(Collectors.toList()));
        // if we want more than 1 level
        // lrFiltersLinks = AbstractSymbolicTable.genConjunctiveFilters(this, lrFiltersLinks.getKey().stream().collect(Collectors.toList()));

        Map<BVFilter, BVFilter> promotedFilters1 = new HashMap<>();
        Map<BVFilter, BVFilter> promotedFilters2 = new HashMap<>();

        for (BVFilter f1 : st1FiltersLinks) {
            promotedFilters1.put(f1, this.pullUpLeftFilter(f1));
        }
        for (BVFilter f2 : st2FiltersLinks) {
            promotedFilters2.put(f2, this.pullUpRightFilter(f2));
        }

        int filtersToBeVisited = promotedFilters1.keySet().size() * promotedFilters2.keySet().size() * lrFiltersLinks.size();
        // System.out.println("Filters to be visited: " + filtersToBeVisited);

        Statistics.compound_case_cnt ++;
        Statistics.sum_compound_filter_cnt += filtersToBeVisited;
        Statistics.max_compound_filter_cnt = Statistics.max_compound_filter_cnt > filtersToBeVisited ? Statistics.max_compound_filter_cnt : filtersToBeVisited;

        int tt = 0;
        for (BVFilter f1 : st1FiltersLinks) {
            for (BVFilter f2 : st2FiltersLinks) {

                BVFilter mergedf1f2 = BVFilter.mergeFilterConj(
                        promotedFilters1.get(f1),
                        promotedFilters2.get(f2));

                for (BVFilter lrf : lrFiltersLinks) {

                    tt ++;

                    BVFilter mergedFilter = BVFilter
                            .mergeFilterConj(mergedf1f2, lrf);

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

        this.allFiltersEnumerated = true;
        this.totalBitVecFiltersCount = instantiatedFilters.size();
        return instantiatedFilters;
    }

    @Override
    public int getPrimitiveFilterNum() {
        return this.filtersLR.size();
    }

    @Override
    void encodePrimitiveFilters(EnumContext ec) {

        if (this.primitiveFiltersEvaluated) return;

        // first evaluate the filters for sub-abstract tables
        st1.encodePrimitiveFilters(ec);
        st2.encodePrimitiveFilters(ec);

        // if the composition type is union, no LR filter exist
        if (this.compositionType == CompositionType.union) {
            // make sure that the empty filter is added

            List<Filter> filters = new ArrayList<>();

            filters.add(new EmptyFilter());
            BVFilter bv = BVFilter.genEmptyFilter(this.getBaseTable().getContent().size());
            decodedLR.put(bv, new Pair<>(0., Arrays.asList(new EmptyFilter())));
            filtersLR.add(bv);

            this.primitiveFiltersEvaluated = true;
            this.primitiveBitVecFilterCount = this.filtersLR.size();
            this.primitiveSynFilterCount = filters.size();

            return;
        } else if (this.compositionType == CompositionType.join) {

            JoinNode jn = new JoinNode(
                    Arrays.asList(
                            new NamedTable(this.st1.getBaseTable()),
                            new NamedTable(this.st2.getBaseTable())));

            RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
            this.representativeTableNode = rt;

            // the maximum filter length for filtersLR should be 1,
            // we use a temp variable to store the original maximum filter length
            int backUpMaxFilterLength = ec.getMaxFilterLength();
            ec.setMaxFilterLength(1);
            List<Filter> filters = FilterEnumerator.enumCanonicalFilterJoinNode(rt, ec);
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
                                representativeTableNode.getSchema(),
                                representativeTableNode.originalColumnName()));
                if (cost < entryRef.getKey()) {
                    decodedLR.put(symFilter, new Pair<>(cost, entryRef.getValue()));
                }
            }

            this.primitiveFiltersEvaluated = true;
            this.primitiveBitVecFilterCount = this.filtersLR.size();
            this.primitiveSynFilterCount = filters.size();

            if (GlobalConfig.STAT_MODE) {
                Set<BVFilter> conj = AbstractSummaryTable.genConjunctiveFilters(this, this.filtersLR.stream().collect(Collectors.toList()));
                System.out.println("Origin: " + ((filters.size() * filters.size() + filters.size())) + "   Now: " + conj.size());
                System.out.println("[CFilter Reduction Rate] " + ((double) (filters.size() * filters.size() + filters.size())) / conj.size());
            }

            // calculating the reduction rate from bit vector to syntax filters
            // System.out.println("#(BitVec)/#(SynFilter): " + this.filtersLR.size() + " / " + filters.size() + " = " + (((float)filtersLR.size()) / filters.size()));
            Statistics.red_syn_to_bv_case_cnt ++;
            Statistics.sum_red_syn_to_bv += ((float) filters.size()) / ((float)filtersLR.size());
        }
    }


    // Promote a filter in left table to a filter for current baseTable
    private BVFilter pullUpLeftFilter(BVFilter sf1) {

        if (this.compositionType.equals(CompositionType.join)) {
            Set<Integer> pulledFilter = new HashSet<>();
            for (int i : sf1.getFilterRep()) {
                for (int j  = 0; j < this.st2.getBaseTable().getContent().size(); j ++) {
                    pulledFilter.add(i * this.st2.getBaseTable().getContent().size() + j);
                }
            }
            return new BVFilter(pulledFilter, this.getBaseTable().getContent().size());
        } else if (this.compositionType.equals(CompositionType.union)) {
            Set<Integer> pulledFilter = new HashSet<>();
            pulledFilter.addAll(sf1.getFilterRep());
            for (int j  = 0; j < this.st2.getBaseTable().getContent().size(); j ++) {
                pulledFilter.add(j + st1.getBaseTable().getContent().size());
            }
            // we don't need to change filter value but the row number of the filter is different now
            return new BVFilter(pulledFilter, this.getBaseTable().getContent().size());
        }
        return null;
    }

    // Promote a filter in right table to a filter for current baseTable
    private BVFilter pullUpRightFilter(BVFilter sf2) {
        if (this.compositionType.equals(CompositionType.join)) {
            Set<Integer> pulledFilter = new HashSet<>();
            for (int i = 0; i < st1.getBaseTable().getContent().size(); i ++) {
                for (int j : sf2.getFilterRep()) {
                    pulledFilter.add(i * this.st2.getBaseTable().getContent().size() + j);
                }
            }
            return new BVFilter(pulledFilter, this.getBaseTable().getContent().size());
        } else if (this.compositionType.equals(CompositionType.union)) {
            Set<Integer> pulledFilter = new HashSet<>();
            for (int i = 0; i < this.st1.getBaseTable().getContent().size(); i ++) {
                pulledFilter.add(i);
            }
            for (int j : sf2.getFilterRep()) {
                pulledFilter.add(j + this.st1.getBaseTable().getContent().size());
            }
            return new BVFilter(pulledFilter, this.getBaseTable().getContent().size());
        }
        return null;
    }

    public List<Filter> decodeLR(BVFilter sf) {
        return decodedLR.get(sf).getValue();
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
                .stream().map(x -> x + st1.getBaseTable().getSchema().size()).collect(Collectors.toList());
        result.addAll(leftBoundary);
        result.addAll(rightBoundary);
        return result;
    }

    @Override
    public Map<BVFilter, List<BVFilterCompTree>> batchGenDecomposition(Set<BVFilter> targets) {

        Set<BVFilter> st1Filters = st1.instantiateAllFilters();
        Set<BVFilter> st2Filters = st2.instantiateAllFilters();
        Set<BVFilter> lrFilters =
                AbstractSummaryTable.genConjunctiveFilters(this, this.filtersLR.stream().collect(Collectors.toList()));

        Map<BVFilter, BVFilter> promotedFilters1 = new HashMap<>();
        Map<BVFilter, BVFilter> promotedFilters2 = new HashMap<>();

        for (BVFilter f1 : st1Filters) {
            BVFilter promotedf1 = this.pullUpLeftFilter(f1);
            if (fullyContainedAnElement(promotedf1, targets))
                promotedFilters1.put(f1, promotedf1);
        }
        for (BVFilter f2 : st2Filters) {
            BVFilter promotedf2 = this.pullUpRightFilter(f2);
            if (fullyContainedAnElement(promotedf2, targets))
                promotedFilters2.put(f2, promotedf2);
        }

        Set<BVFilter> validLRFilters = new HashSet<>();
        for (BVFilter sf : lrFilters) {
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

                BVFilter mergef1f2 = BVFilter.mergeFilterConj(
                        promotedFilters1.get(f1), promotedFilters2.get(f2));

                if (! fullyContainedAnElement(mergef1f2, targets))
                    continue;

                for (BVFilter lrf : validLRFilters) {

                    BVFilter mergef1f2lr = BVFilter.mergeFilterConj(
                            mergef1f2, lrf);

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
                BVFilter mergedij = BVFilter.mergeFilterConj(usefulPrimitive.get(i),
                        usefulPrimitive.get(j));
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

    @Override
    public List<NamedTable> namedTableInvolved() {
        List<NamedTable> result = new ArrayList<>();
        result.addAll(st1.namedTableInvolved());
        result.addAll(st2.namedTableInvolved());
        return result;
    }

}
