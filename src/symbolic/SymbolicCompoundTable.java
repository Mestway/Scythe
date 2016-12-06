package symbolic;

import enumerator.context.EnumContext;
import enumerator.primitive.EnumCanonicalFilters;
import global.Statistics;
import mapping.CoordInstMap;
import mapping.MappingInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import util.CombinationGenerator;
import util.DebugHelper;
import util.Pair;
import util.RenameTNWrapper;

import java.time.Clock;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by clwang on 3/26/16.
 */
public class SymbolicCompoundTable extends AbstractSymbolicTable {

    public AbstractSymbolicTable st1, st2;
    // the set of filters that use both st1 elements and st2 elements,
    // original filters on st1 and st2 are not contained here
    Set<SymbolicFilter> filtersLR = new HashSet<>();
    // this list will be ready after primitiveFiltersEvaluated is evaluate to true.
    List<SymbolicFilter> primitives = new ArrayList<>();

    private boolean primitiveFiltersEvaluated = false;
    // the representitive table will not be available until primitives are evaluated
    // this tableNode represent the tablenode used in evaluating the filters
    RenameTableNode representitiveTableNode = null;

    private SymbolicCompoundTable() {}

    public SymbolicCompoundTable(AbstractSymbolicTable st1,  AbstractSymbolicTable st2) {
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

    @Deprecated
    // This function will instantiate all filters in a on the fly manner
    public Pair<Set<SymbolicFilter>, FilterLinks> instantiateAllFiltersLazily() {

        Set<SymbolicFilter> result = new HashSet<>();
        FilterLinks fl = new FilterLinks();

        int k = 0;
        while (true) {
            Optional<Pair<SymbolicFilter, FilterLinks>> op = this.lazyFilterEval(k);
            k ++;
            if (! op.isPresent()) break;
            result.add(op.get().getKey());
            fl = FilterLinks.merge(Arrays.asList(fl, op.get().getValue()));
        }

        // this is used to make sure that the empty filter is added
        result.add(SymbolicFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter()));

        return new Pair<>(result, fl);
    }

    // an version 2 that visits the tables with multi-level demotion
    public void emitFinalVisitAllTables(
            MappingInference mi,
            EnumContext ec,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {
        this.evalPrimitive(ec);

        Set<SymbolicFilter> dummyExtFilter = new HashSet<>();
        dummyExtFilter.add(SymbolicFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter()));

        Set<Pair<SymbolicFilter, SymbolicFilter>> r = this.visitDemotedSpace(ec, dummyExtFilter);

        for (Pair<SymbolicFilter, SymbolicFilter> p : r) {
            f.accept(new Pair<>(this, p.getKey()), new FilterLinks());
        }
    }

    // TODO: deal with symbolic compound table visit smartly
    public void emitFinalVisitAllTablesV2(
            MappingInference mi,
            EnumContext ec,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {

        /****
         * The first part, deal with the situation that the output table contains values from both st1 and st2,
         *  in this case, we will directly use backward inferred mapping to guide search
         */

        this.evalPrimitive(ec);
        //emitVisitCrossTableMappingFilters(mi, f);

        // visit demoted mapping space
        emitVisitDemotedMappingFilters(ec, f);

        // TODO: think carefully about this, we may need a proof to show that we need *all* boundires
        List<CoordInstMap> map = mi.genMappingInstancesWColumnBarrier(this.getTableRightIndexBoundries());
        //List<CoordInstMap> map = mi.genMappingInstances();

        System.out.println("[Cross Table Mapping Size] " + map.size());

        Set<SymbolicFilter> targetFilters = new HashSet<>();
        for (CoordInstMap cim : map) {
            SymbolicFilter sf = new SymbolicFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        this.evalPrimitive(ec);
        Pair<Set<SymbolicFilter>, FilterLinks> p = this.lastStageInstantiateAllFilters(targetFilters);
        for (SymbolicFilter spf : p.getKey()) {
            f.accept(new Pair<>(this, spf), p.getValue());
        }

        if (targetFilters.size() == 0)
            return;

        Statistics.sum_back_filter_bogus_rate += ((float)(targetFilters.size() - p.getKey().size())) / targetFilters.size();
        Statistics.back_filter_bogus_cases ++;

    }

    // given the mapping inference data structure, we only try to use evaluate those mapping
    // TODO: This one is somehow less efficient than simply traversing.
    public void emitVisitCrossTableMappingFilters(
            MappingInference mi,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {

        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        // construct the target filters that we need, inferred backwardly
        List<CoordInstMap> map = mi.genMappingInstancesWColumnBarrier(this.getTableRightIndexBoundries());

        Set<SymbolicFilter> targetFilters = new HashSet<>();
        for (CoordInstMap cim : map) {
            SymbolicFilter sf = new SymbolicFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        // Infer the set of LRFilters we need by only looking into LRFilters.
        //   (Since the most important source of creating join is this type of LR filters)
        Pair<Set<SymbolicFilter>, FilterLinks> lrFiltersLinks =
                AbstractSymbolicTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));

        Set<SymbolicFilter> validLRFilters = new HashSet<>();
        for (SymbolicFilter sf : lrFiltersLinks.getKey()) {
            if (fullyContainedAnElement(sf, targetFilters))
                validLRFilters.add(sf);
        }

        // map each filter in the sub-symbolic table to its promoted form in the cartesian product table.
        Pair<Set<SymbolicFilter>, FilterLinks> st1FiltersLinks = st1.instantiateAllFilters();
        Pair<Set<SymbolicFilter>, FilterLinks> st2FiltersLinks = st2.instantiateAllFilters();

        Map<SymbolicFilter, SymbolicFilter> promotedFilters1 = new HashMap<>();
        Map<SymbolicFilter, SymbolicFilter> promotedFilters2 = new HashMap<>();

        Set<SymbolicFilter> leftDemotedFilters
                = targetFilters.stream().map(tf -> this.demoteToLeftFilter(tf)).collect(Collectors.toSet());
        for (SymbolicFilter f1 : st1FiltersLinks.getKey()) {
            if (fullyContainedAnElement(f1, leftDemotedFilters)) {
                SymbolicFilter promotedf1 = this.promoteLeftFilter(f1);
                promotedFilters1.put(f1, promotedf1);
            }
        }
        Set<SymbolicFilter> rightDemotedFilters
                = targetFilters.stream().map(tf -> this.demoteToRightFilter(tf)).collect(Collectors.toSet());
        for (SymbolicFilter f2 : st2FiltersLinks.getKey()) {
            if (fullyContainedAnElement(f2, rightDemotedFilters)) {
                SymbolicFilter promotedf2 = this.promoteRightFilter(f2);
                promotedFilters2.put(f2, promotedf2);
            }
        }

        // FilterLinks necessary to construct filters we want, and store this around to print filters
        FilterLinks filterLinks = new FilterLinks();
        Set<SymbolicFilter> instantiatedFilters = new HashSet<>();

        // For statistics
        int filtersSupposedToBeVisited = st1FiltersLinks.getKey().size() * st2FiltersLinks.getKey().size() * lrFiltersLinks.getKey().size();
        Statistics.compound_case_cnt ++;
        Statistics.sum_compound_filter_cnt += filtersSupposedToBeVisited;
        Statistics.max_compound_filter_cnt = Statistics.max_compound_filter_cnt > filtersSupposedToBeVisited ? Statistics.max_compound_filter_cnt : filtersSupposedToBeVisited;

        int tt = 0;
        for (SymbolicFilter lrf : validLRFilters) {
            if (targetFilters.contains(lrf)) {

                SymbolicFilter emptyF1 = SymbolicFilter.genSymbolicFilter(st1.getBaseTable(), new EmptyFilter());
                SymbolicFilter emptyF2 = SymbolicFilter.genSymbolicFilter(st2.getBaseTable(), new EmptyFilter());

                Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                src.add(new Pair<>(st1, emptyF1));
                src.add(new Pair<>(st2, emptyF2));
                src.add(new Pair<>(this, lrf));
                filterLinks.addLink(src, new Pair<>(this, lrf));

                Pair<AbstractSymbolicTable, SymbolicFilter> p1 = new Pair<>(st1, emptyF1);
                Pair<AbstractSymbolicTable, SymbolicFilter> p2 = new Pair<>(st1, emptyF2);

                if (filterLinks.retrieveSource(p1) != null && filterLinks.retrieveSource(p1).isEmpty())
                    filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p1), p1);
                if (filterLinks.retrieveSource(p2) != null && filterLinks.retrieveSource(p2).isEmpty())
                    filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p2), p2);

                f.accept(new Pair<>(this, lrf), filterLinks);

                tt ++;

                continue;
            }

            for (SymbolicFilter f1 : promotedFilters1.keySet()) {

                SymbolicFilter mergedlrf1 = SymbolicFilter.mergeFilter(
                        lrf,
                        promotedFilters1.get(f1),
                        AbstractSymbolicTable.mergeFunction);

                if (targetFilters.contains(mergedlrf1)) {

                    SymbolicFilter emptyF2 = SymbolicFilter.genSymbolicFilter(st2.getBaseTable(), new EmptyFilter());

                    Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                    src.add(new Pair<>(st1, f1));
                    src.add(new Pair<>(st2, emptyF2));
                    src.add(new Pair<>(this, lrf));
                    filterLinks.addLink(src, new Pair<>(this, lrf));

                    Pair<AbstractSymbolicTable, SymbolicFilter> p1 = new Pair<>(st1, f1);
                    Pair<AbstractSymbolicTable, SymbolicFilter> p2 = new Pair<>(st1, emptyF2);

                    if (filterLinks.retrieveSource(p1) != null && filterLinks.retrieveSource(p1).isEmpty())
                        filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p1), p1);
                    if (filterLinks.retrieveSource(p2) != null && filterLinks.retrieveSource(p2).isEmpty())
                        filterLinks.setLinkSource(st2FiltersLinks.getValue().retrieveSource(p2), p2);

                    f.accept(new Pair<>(this, mergedlrf1), filterLinks);

                    tt ++;

                    continue;
                }

                for (SymbolicFilter f2 : promotedFilters2.keySet()) {
                    SymbolicFilter mergedlrf1f2 = SymbolicFilter.mergeFilter(
                            mergedlrf1,
                            promotedFilters2.get(f2),
                            AbstractSymbolicTable.mergeFunction);

                    if (targetFilters.contains(mergedlrf1f2)) {

                        Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                        src.add(new Pair<>(st1, f1));
                        src.add(new Pair<>(st2, f2));
                        src.add(new Pair<>(this, lrf));
                        filterLinks.addLink(src, new Pair<>(this, lrf));

                        Pair<AbstractSymbolicTable, SymbolicFilter> p1 = new Pair<>(st1, f1);
                        Pair<AbstractSymbolicTable, SymbolicFilter> p2 = new Pair<>(st1, f2);

                        if (filterLinks.retrieveSource(p1) != null && filterLinks.retrieveSource(p1).isEmpty())
                            filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p1), p1);
                        if (filterLinks.retrieveSource(p2) != null && filterLinks.retrieveSource(p2).isEmpty())
                            filterLinks.setLinkSource(st2FiltersLinks.getValue().retrieveSource(p2), p2);

                        f.accept(new Pair<>(this, mergedlrf1f2), filterLinks);

                        tt ++;
                        continue;
                    }
                }
            }
        }

        Statistics.sum_back_infer_filter_size += targetFilters.size();
        Statistics.max_back_infer_filter_size = Statistics.max_back_infer_filter_size > targetFilters.size() ?
                Statistics.max_back_infer_filter_size : targetFilters.size();
        Statistics.sum_red_compound_to_bv += (tt / ((float)instantiatedFilters.size()));
        Statistics.sum_last_stage_visited_filter_cnt += tt;
        Statistics.sum_last_stage_compound_filter_cnt += filtersSupposedToBeVisited;
        Statistics.sum_last_stage_red_visited_compound += ((float)tt) / filtersSupposedToBeVisited;
        Statistics.last_stage_compound_case_cnt ++;

        this.allfiltersEnumerated = true;
        this.totalBitVecFiltersCount = instantiatedFilters.size();
    }

    // emit visit cases that mappings are demoted to mappings from the left table
    public void emitVisitDemotedMappingFilters(
            EnumContext ec,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {

        MappingInference mi = MappingInference.buildMapping(this.st1.getBaseTable(), ec.getOutputTable());
        if (! mi.isValidMapping())
            return;

        // construct the target filters that we need, inferred backwardly
        List<CoordInstMap> map = mi.genMappingInstances();

        System.out.println("[Demoted Mapping Size] " + map.size());

        Set<SymbolicFilter> targetFilters = new HashSet<>();
        for (CoordInstMap cim : map) {
            SymbolicFilter sf = new SymbolicFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        // Infer the set of LRFilters we need by only looking into LRFilters.
        //   (Since the most important source of creating join is this type of LR filters)
        Pair<Set<SymbolicFilter>, FilterLinks> lrFiltersLinks =
                AbstractSymbolicTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));

        Map<SymbolicFilter, SymbolicFilter> leftDemotedLRMap = new HashMap<>();

        Set<SymbolicFilter> validLRFilters = new HashSet<>();
        for (SymbolicFilter sf : lrFiltersLinks.getKey()) {
            SymbolicFilter demoted = this.demoteToLeftFilter(sf);
            if (fullyContainedAnElement(demoted, targetFilters)) {
                validLRFilters.add(sf);
                leftDemotedLRMap.put(sf, demoted);
            }
        }

        Pair<Set<SymbolicFilter>, FilterLinks> st1FiltersLinks = st1.instantiateAllFilters();
        Pair<Set<SymbolicFilter>, FilterLinks> st2FiltersLinks = st2.instantiateAllFilters();

        Map<SymbolicFilter, SymbolicFilter> promotedFilters2 = new HashMap<>();
        for (SymbolicFilter sf : st2FiltersLinks.getKey()) {
            promotedFilters2.put(sf, this.promoteRightFilter(sf));
        }

        Set<SymbolicFilter> validST1Filters = new HashSet<>();
        for (SymbolicFilter f1 : st1FiltersLinks.getKey()) {
            if (! fullyContainedAnElement(f1, targetFilters))
                continue;
            validST1Filters.add(f1);
        }

        FilterLinks filterLinks = new FilterLinks();

        int tt = 0;
        for (SymbolicFilter lrf : validLRFilters) {

            if (targetFilters.contains(leftDemotedLRMap.get(lrf))) {

                SymbolicFilter emptyF1 = SymbolicFilter.genSymbolicFilter(st1.getBaseTable(), new EmptyFilter());
                SymbolicFilter emptyF2 = SymbolicFilter.genSymbolicFilter(st2.getBaseTable(), new EmptyFilter());

                Pair<AbstractSymbolicTable, SymbolicFilter> p1 = new Pair<>(st1, emptyF1);
                Pair<AbstractSymbolicTable, SymbolicFilter> p2 = new Pair<>(st1, emptyF2);

                Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                src.add(new Pair<>(st1, emptyF1));
                src.add(new Pair<>(st2, emptyF2));
                src.add(new Pair<>(this, lrf));
                filterLinks.addLink(src, new Pair<>(this, lrf));

                if (filterLinks.retrieveSource(p1) != null && filterLinks.retrieveSource(p1).isEmpty())
                    filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p1), p1);
                if (filterLinks.retrieveSource(p2) != null && filterLinks.retrieveSource(p2).isEmpty())
                    filterLinks.setLinkSource(st2FiltersLinks.getValue().retrieveSource(p2), p2);

                f.accept(new Pair<>(this, lrf), filterLinks);
                tt ++;
                continue;
            }

            for (SymbolicFilter f1 : validST1Filters) {

                SymbolicFilter mergedlrf1 = SymbolicFilter.mergeFilter(
                        demoteToLeftFilter(lrf),
                        f1,
                        AbstractSymbolicTable.mergeFunction);

                if (! fullyContainedAnElement(mergedlrf1, targetFilters))
                    continue;

                if (targetFilters.contains(mergedlrf1)) {

                    SymbolicFilter emptyF2 = SymbolicFilter.genSymbolicFilter(st2.getBaseTable(), new EmptyFilter());

                    Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                    src.add(new Pair<>(st1, this.promoteLeftFilter(f1)));
                    src.add(new Pair<>(st2, emptyF2));
                    src.add(new Pair<>(this, lrf));
                    filterLinks.addLink(src, new Pair<>(this, lrf));

                    Pair<AbstractSymbolicTable, SymbolicFilter> p1 = new Pair<>(st1, f1);
                    Pair<AbstractSymbolicTable, SymbolicFilter> p2 = new Pair<>(st1, emptyF2);

                    if (filterLinks.retrieveSource(p1) != null && filterLinks.retrieveSource(p1).isEmpty())
                        filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p1), p1);
                    if (filterLinks.retrieveSource(p2) != null && filterLinks.retrieveSource(p2).isEmpty())
                        filterLinks.setLinkSource(st2FiltersLinks.getValue().retrieveSource(p2), p2);

                    f.accept(new Pair<>(this, mergedlrf1), filterLinks);

                    tt ++;

                    continue;
                }

                for (SymbolicFilter f2 : promotedFilters2.keySet()) {

                    SymbolicFilter lrf2 = SymbolicFilter.mergeFilter(
                            lrf, promotedFilters2.get(f2),
                            AbstractSymbolicTable.mergeFunction);

                    SymbolicFilter demotedlrf2 = this.demoteToLeftFilter(lrf2);

                    if (! fullyContainedAnElement(demotedlrf2, targetFilters))
                        continue;

                    SymbolicFilter mergedlrf1f2 = SymbolicFilter.mergeFilter(
                            f1, demotedlrf2,
                            AbstractSymbolicTable.mergeFunction);

                    if (targetFilters.contains(mergedlrf1f2)) {

                        Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                        src.add(new Pair<>(st1, this.promoteLeftFilter(f1)));
                        src.add(new Pair<>(st2, promotedFilters2.get(f2)));
                        src.add(new Pair<>(this, lrf));
                        filterLinks.addLink(src, new Pair<>(this, lrf));

                        Pair<AbstractSymbolicTable, SymbolicFilter> p1 = new Pair<>(st1, f1);
                        Pair<AbstractSymbolicTable, SymbolicFilter> p2 = new Pair<>(st1, f2);

                        if (filterLinks.retrieveSource(p1) != null && filterLinks.retrieveSource(p1).isEmpty())
                            filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p1), p1);
                        if (filterLinks.retrieveSource(p2) != null && filterLinks.retrieveSource(p2).isEmpty())
                            filterLinks.setLinkSource(st2FiltersLinks.getValue().retrieveSource(p2), p2);

                        f.accept(new Pair<>(this, mergedlrf1f2), filterLinks);

                        tt ++;
                        continue;
                    }
                }
            }
        }
    }

    // TODO: implement visit demoted space
    // The result is a set of pairs p = (sf, ef),
    //  where sf is the a filter generated from combining f1*f2*lrf and ef is a filter from demotedExtFilters
    //  p is the pair that satisfies the property sf*ef is a filter that can generate output
    public Set<Pair<SymbolicFilter, SymbolicFilter>> visitDemotedSpace(
            EnumContext ec, Set<SymbolicFilter> demotedExtFilters) {

        Set<Pair<SymbolicFilter, SymbolicFilter>> resultPairs = new HashSet<>();

        MappingInference mi = MappingInference.buildMapping(this.getBaseTable(), ec.getOutputTable());
        Set<SymbolicFilter> targetFilters = new HashSet<>();

        // construct the target filters backwardly
        List<CoordInstMap> map = mi.genMappingInstancesWColumnBarrier(this.getTableRightIndexBoundries());

        System.out.println("[CrossTableMapping Instances] " + map.size());
        for (CoordInstMap cim : map) {
            SymbolicFilter sf = new SymbolicFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        Pair<Set<SymbolicFilter>, FilterLinks> st1FiltersLinks = st1.instantiateAllFilters();
        Pair<Set<SymbolicFilter>, FilterLinks> st2FiltersLinks = st2.instantiateAllFilters();
        Pair<Set<SymbolicFilter>, FilterLinks> lrFiltersLinks =
                AbstractSymbolicTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));

        // map each filter in the sub-symbolic table to its promoted form in the cartesian product table.
        Map<SymbolicFilter, SymbolicFilter> promotedFilters1 = new HashMap<>();
        Map<SymbolicFilter, SymbolicFilter> promotedFilters2 = new HashMap<>();
        for (SymbolicFilter f1 : st1FiltersLinks.getKey()) {
            SymbolicFilter promotedf1 = this.promoteLeftFilter(f1);
            if (fullyContainedAnElement(promotedf1, targetFilters))
                promotedFilters1.put(f1, promotedf1);
        }
        for (SymbolicFilter f2 : st2FiltersLinks.getKey()) {
            SymbolicFilter promotedf2 = this.promoteRightFilter(f2);
            if (fullyContainedAnElement(promotedf2, targetFilters))
                promotedFilters2.put(f2, promotedf2);
        }
        Set<SymbolicFilter> validLRFilters = new HashSet<>();
        for (SymbolicFilter sf : lrFiltersLinks.getKey()) {
            if (fullyContainedAnElement(sf, targetFilters))
                validLRFilters.add(sf);
        }

        // In the first part, we are trying to visit filters to infer filters that can yield to a cross table filtering
        for (SymbolicFilter lrf : validLRFilters) {
            for (SymbolicFilter f2 : promotedFilters2.keySet()) {
                SymbolicFilter mergedlrf2 = SymbolicFilter.mergeFilter(
                        lrf, promotedFilters2.get(f2), AbstractSymbolicTable.mergeFunction);
                if (! fullyContainedAnElement(mergedlrf2, targetFilters))
                    continue;

                for (SymbolicFilter ef : demotedExtFilters) {
                    SymbolicFilter mergedlrf2ef = SymbolicFilter.mergeFilter(
                            mergedlrf2, ef,
                            AbstractSymbolicTable.mergeFunction
                    );

                    if (! fullyContainedAnElement(mergedlrf2ef, targetFilters))
                        continue;

                    for (SymbolicFilter f1 : promotedFilters1.keySet()) {
                        SymbolicFilter mergedlrf2eff1 = SymbolicFilter.mergeFilter(
                                mergedlrf2ef, promotedFilters1.get(f1),
                                AbstractSymbolicTable.mergeFunction
                        );
                        // Add the pair to the result set.
                        if (! targetFilters.contains(mergedlrf2eff1))
                            continue;

                        resultPairs.add(new Pair<>(
                                SymbolicFilter.mergeFilter(mergedlrf2, promotedFilters1.get(f1), AbstractSymbolicTable.mergeFunction),
                                ef));
                    }
                }
            }
        }

        MappingInference st1Mi = MappingInference.buildMapping(this.st1.getBaseTable(), ec.getOutputTable());
        boolean furtherDemotionToSt1 = st1Mi.isValidMapping();

        // If we cannot perform further demotion
        if (! furtherDemotionToSt1) return resultPairs;

        Set<SymbolicFilter> st1TargetFilters = new HashSet<>();
        List<CoordInstMap> st1Map = st1Mi.genMappingInstances();
        System.out.println("[demotedFilter Mapping Instances] " + st1Map.size());

        for (CoordInstMap cim : st1Map) {
            SymbolicFilter sf = new SymbolicFilter(cim.rowsInvolved(), st1.getBaseTable().getContent().size());
            st1TargetFilters.add(sf);
        }

        // map each filter in the sub-symbolic table to its promoted form in the cartesian product table.
        Map<SymbolicFilter, SymbolicFilter> validPromotedDemotedf2 = new HashMap<>();
        for (SymbolicFilter f2 : st2FiltersLinks.getKey()) {
            SymbolicFilter promotedf2 = this.promoteRightFilter(f2);
            SymbolicFilter lDemotedf2 = this.demoteToLeftFilter(promotedf2);
            if (fullyContainedAnElement(lDemotedf2, st1TargetFilters))
                validPromotedDemotedf2.put(f2, promotedf2);
        }
        Map<SymbolicFilter, SymbolicFilter> validLRDemoted = new HashMap<>();
        for (SymbolicFilter sf : lrFiltersLinks.getKey()) {
            SymbolicFilter lDemotedLRF = this.demoteToLeftFilter(sf);
            if (fullyContainedAnElement(lDemotedLRF, st1TargetFilters))
                validLRDemoted.put(sf, lDemotedLRF);
        }
        Map<SymbolicFilter, SymbolicFilter> validExtDemoted = new HashMap<>();
        for (SymbolicFilter ef : demotedExtFilters) {
            SymbolicFilter lDemotedEF = this.demoteToLeftFilter(ef);
            if (fullyContainedAnElement(lDemotedEF, st1TargetFilters))
                validExtDemoted.put(ef, lDemotedEF);
        }

        // TODO: think carefully whether we should store all these mappings
        Map<SymbolicFilter, Set<Pair<SymbolicFilter, SymbolicFilter>>> demotedCompoundToOriginalFilter = new HashMap<>();

        for (SymbolicFilter lrf : validLRDemoted.keySet()) {
            for (SymbolicFilter f2 : validPromotedDemotedf2.keySet()) {
                SymbolicFilter mergedlrf2 = SymbolicFilter.mergeFilter(
                        lrf, validPromotedDemotedf2.get(f2), AbstractSymbolicTable.mergeFunction);
                if (! fullyContainedAnElement(demoteToLeftFilter(mergedlrf2), st1TargetFilters))
                    continue;
                for (SymbolicFilter ef : validExtDemoted.keySet()) {
                    SymbolicFilter mergedlrf2ef = SymbolicFilter.mergeFilter(
                            mergedlrf2, ef,
                            AbstractSymbolicTable.mergeFunction
                    );

                    SymbolicFilter lDemotedMergedlrf2ef = demoteToLeftFilter(mergedlrf2ef);
                    if (! fullyContainedAnElement(lDemotedMergedlrf2ef, st1TargetFilters))
                        continue;

                    Pair<SymbolicFilter, SymbolicFilter> p = new Pair<>(mergedlrf2, ef);

                    if (! demotedCompoundToOriginalFilter.containsKey(lDemotedMergedlrf2ef)) {
                        demotedCompoundToOriginalFilter.put(lDemotedMergedlrf2ef, new HashSet<>());
                    }
                    demotedCompoundToOriginalFilter.get(lDemotedMergedlrf2ef).add(p);
                }
            }
        }

        Set<Pair<SymbolicFilter, SymbolicFilter>> demotedEvalResult = st1.visitDemotedSpace(ec, demotedCompoundToOriginalFilter.keySet());

        // the first element of p is a filter from st1, second element is a demoted filter
        for (Pair<SymbolicFilter, SymbolicFilter> p : demotedEvalResult) {
            SymbolicFilter promotedF1 = this.promoteLeftFilter(p.getKey());

            // key of p2 is mergedf2lr, value of p2 is a extFilter
            for (Pair<SymbolicFilter, SymbolicFilter> p2 : demotedCompoundToOriginalFilter.get(p.getValue())) {
                SymbolicFilter mergedf1f2lr = SymbolicFilter.mergeFilter(promotedF1, p2.getKey(),
                        AbstractSymbolicTable.mergeFunction);

                resultPairs.add(new Pair<>(mergedf1f2lr, p2.getValue()));
            }
        }

        return resultPairs;
    }

    @Override
    public Pair<Set<SymbolicFilter>, FilterLinks> instantiateAllFilters() {

        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        //Map<SymbolicFilter, Integer> hmp = new HashMap<>();
        Set<SymbolicFilter> instantiatedFilters = new HashSet<>();

        Pair<Set<SymbolicFilter>, FilterLinks> st1FiltersLinks = st1.instantiateAllFilters();
        Pair<Set<SymbolicFilter>, FilterLinks> st2FiltersLinks = st2.instantiateAllFilters();

        Pair<Set<SymbolicFilter>, FilterLinks> lrFiltersLinks =
                AbstractSymbolicTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));
        // if we want more than 1 level
        // lrFiltersLinks = AbstractSymbolicTable.mergeAndLinkFilters(this, lrFiltersLinks.getKey().stream().collect(Collectors.toList()));

        // currently keep all filters with me, export when needed
        FilterLinks filterLinks = FilterLinks.merge(
                Arrays.asList(
                        st1FiltersLinks.getValue(),
                        st2FiltersLinks.getValue(),
                        lrFiltersLinks.getValue()));

        Map<SymbolicFilter, SymbolicFilter> promotedFilters1 = new HashMap<>();
        Map<SymbolicFilter, SymbolicFilter> promotedFilters2 = new HashMap<>();

        for (SymbolicFilter f1 : st1FiltersLinks.getKey()) {
            promotedFilters1.put(f1, this.promoteLeftFilter(f1));
        }
        for (SymbolicFilter f2 : st2FiltersLinks.getKey()) {
            promotedFilters2.put(f2, this.promoteRightFilter(f2));
        }

        int filtersToBeVisited = promotedFilters1.keySet().size() * promotedFilters2.keySet().size() * lrFiltersLinks.getKey().size();
        // System.out.println("Filters to be visited: " + filtersToBeVisited);

        Statistics.compound_case_cnt ++;
        Statistics.sum_compound_filter_cnt += filtersToBeVisited;
        Statistics.max_compound_filter_cnt = Statistics.max_compound_filter_cnt > filtersToBeVisited ? Statistics.max_compound_filter_cnt : filtersToBeVisited;

        int tt = 0;
        for (SymbolicFilter f1 : st1FiltersLinks.getKey()) {
            for (SymbolicFilter f2 : st2FiltersLinks.getKey()) {

                SymbolicFilter mergedf1f2 = SymbolicFilter.mergeFilter(
                        promotedFilters1.get(f1),
                        promotedFilters2.get(f2),
                        AbstractSymbolicTable.mergeFunction);

                for (SymbolicFilter lrf : lrFiltersLinks.getKey()) {

                    tt ++;

                    SymbolicFilter mergedFilter = SymbolicFilter
                            .mergeFilter(mergedf1f2, lrf, AbstractSymbolicTable.mergeFunction);

                    instantiatedFilters.add(mergedFilter);

                    if (tt % 100000 == 1000)
                        System.out.println("stored cnt / visited compound cnt " + instantiatedFilters.size() + " / " + tt + "(" + filtersToBeVisited + ")" + " = " + instantiatedFilters.size() / ((float) tt));

                    Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                    src.add(new Pair<>(st1, f1));
                    src.add(new Pair<>(st2, f2));
                    src.add(new Pair<>(this, lrf));
                    filterLinks.addLink(src, new Pair<>(this, mergedFilter));
                }
            }
        }

        // System.out.println("#(BitVec)/#(CompoundFilter): " + instantiatedFilters.size() + " / " + tt + " = " + (((float)instantiatedFilters.size()) / tt));
        Statistics.sum_red_compound_to_bv += (tt / ((float)instantiatedFilters.size()));

        this.allfiltersEnumerated = true;
        this.totalBitVecFiltersCount = instantiatedFilters.size();
        return new Pair<>(instantiatedFilters, filterLinks);
    }

    public Pair<Set<SymbolicFilter>, FilterLinks> lastStageInstantiateAllFilters(Set<SymbolicFilter> targetFilters) {

        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        Pair<Set<SymbolicFilter>, FilterLinks> st1FiltersLinks = st1.instantiateAllFilters();
        Pair<Set<SymbolicFilter>, FilterLinks> st2FiltersLinks = st2.instantiateAllFilters();
        Pair<Set<SymbolicFilter>, FilterLinks> lrFiltersLinks =
                AbstractSymbolicTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));

        // map each filter in the sub-symbolic table to its promoted form in the cartesian product table.
        Map<SymbolicFilter, SymbolicFilter> promotedFilters1 = new HashMap<>();
        Map<SymbolicFilter, SymbolicFilter> promotedFilters2 = new HashMap<>();
        for (SymbolicFilter f1 : st1FiltersLinks.getKey()) {
            SymbolicFilter promotedf1 = this.promoteLeftFilter(f1);
            if (fullyContainedAnElement(promotedf1, targetFilters))
                promotedFilters1.put(f1, promotedf1);
        }
        for (SymbolicFilter f2 : st2FiltersLinks.getKey()) {
            SymbolicFilter promotedf2 = this.promoteRightFilter(f2);
            if (fullyContainedAnElement(promotedf2, targetFilters))
                promotedFilters2.put(f2, promotedf2);
        }
        Set<SymbolicFilter> validLRFilters = new HashSet<>();
        for (SymbolicFilter sf : lrFiltersLinks.getKey()) {
            if (fullyContainedAnElement(sf, targetFilters))
             validLRFilters.add(sf);
        }

        // FilterLinks necessary to construct filters we want, and store this around to print filters
        FilterLinks filterLinks = new FilterLinks();
        Set<SymbolicFilter> instantiatedFilters = new HashSet<>();

        // For statistics
        int filtersToBeVisited = st1FiltersLinks.getKey().size() * st2FiltersLinks.getKey().size() * lrFiltersLinks.getKey().size();
        Statistics.compound_case_cnt ++;
        Statistics.sum_compound_filter_cnt += filtersToBeVisited;
        Statistics.max_compound_filter_cnt = Statistics.max_compound_filter_cnt > filtersToBeVisited ? Statistics.max_compound_filter_cnt : filtersToBeVisited;

        int tt = 0;
        for (SymbolicFilter f1 : promotedFilters1.keySet()) {
            for (SymbolicFilter f2 : promotedFilters2.keySet()) {
                SymbolicFilter mergedf1f2 = SymbolicFilter.mergeFilter(
                        promotedFilters1.get(f1),
                        promotedFilters2.get(f2),
                        AbstractSymbolicTable.mergeFunction);
                if (! fullyContainedAnElement(mergedf1f2, targetFilters))
                    continue;
                for (SymbolicFilter lrf : validLRFilters) {
                    tt ++;
                    SymbolicFilter mergedFilter = SymbolicFilter.mergeFilter(
                            mergedf1f2,
                            lrf,
                            AbstractSymbolicTable.mergeFunction);

                    if (!targetFilters.contains(mergedFilter))
                        continue;

                    instantiatedFilters.add(mergedFilter);

                    Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                    src.add(new Pair<>(st1, f1));
                    src.add(new Pair<>(st2, f2));
                    src.add(new Pair<>(this, lrf));
                    filterLinks.addLink(src, new Pair<>(this, mergedFilter));

                    Pair<AbstractSymbolicTable, SymbolicFilter> p1 = new Pair<>(st1, f1);
                    Pair<AbstractSymbolicTable, SymbolicFilter> p2 = new Pair<>(st1, f1);

                    if (filterLinks.retrieveSource(p1) != null && filterLinks.retrieveSource(p1).isEmpty())
                        filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p1), p1);
                    if (filterLinks.retrieveSource(p2) != null && filterLinks.retrieveSource(p2).isEmpty())
                        filterLinks.setLinkSource(st1FiltersLinks.getValue().retrieveSource(p2), p2);
                }
            }
        }

        Statistics.sum_back_infer_filter_size += targetFilters.size();
        Statistics.max_back_infer_filter_size = Statistics.max_back_infer_filter_size > targetFilters.size() ?
                Statistics.max_back_infer_filter_size : targetFilters.size();
        Statistics.sum_red_compound_to_bv += (tt / ((float)instantiatedFilters.size()));
        Statistics.sum_last_stage_visited_filter_cnt += tt;
        Statistics.sum_last_stage_compound_filter_cnt += filtersToBeVisited;
        Statistics.sum_last_stage_red_visited_compound += ((float)tt) / filtersToBeVisited;
        Statistics.last_stage_compound_case_cnt ++;

        this.allfiltersEnumerated = true;
        this.totalBitVecFiltersCount = instantiatedFilters.size();
        return new Pair<>(instantiatedFilters, filterLinks);
    }

    @Override
    public Optional<Pair<SymbolicFilter, FilterLinks>> lazyFilterEval(Integer index) {

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

        Pair<SymbolicFilter, FilterLinks> p1 = st1.lazyFilterEval(i).get();
        Pair<SymbolicFilter, FilterLinks> p2  = st2.lazyFilterEval(j).get();

        FilterLinks filterLinks = FilterLinks.merge(Arrays.asList(p1.getValue(), p2.getValue()));

        // the following code are used to generate a LR filter

        SymbolicFilter LRFilter = null;

        if (k == 0) {
            LRFilter = SymbolicFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter());
        } else {
            k --;
            Pair<Integer, Integer> xy = this.inverseFilterIndex(this.primitives.size(), k);
            LRFilter = SymbolicFilter.mergeFilter(
                    primitives.get(xy.getKey()),
                    primitives.get(xy.getValue()),
                    AbstractSymbolicTable.mergeFunction);

            // add the link from two source filter to the merged filter itself
            Set<Pair<AbstractSymbolicTable, SymbolicFilter>> srcs = new HashSet<>();
            srcs.add(new Pair<>(this, primitives.get(xy.getKey())));
            srcs.add(new Pair<>(this, primitives.get(xy.getValue())));

            filterLinks.addLink(srcs, new Pair<>(this, LRFilter));
        }

        SymbolicFilter mergedFilter = SymbolicFilter.mergeFilter(
                this.promoteLeftFilter(p1.getKey()),
                SymbolicFilter.mergeFilter(
                        this.promoteRightFilter(p2.getKey()),
                        LRFilter,
                        AbstractSymbolicTable.mergeFunction),
                AbstractSymbolicTable.mergeFunction);

        Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
        src.add(new Pair<>(st1, p1.getKey()));
        src.add(new Pair<>(st2, p2.getKey()));
        src.add(new Pair<>(this, LRFilter));
        filterLinks.addLink(src, new Pair<>(this, mergedFilter));

        return Optional.of(new Pair<>(mergedFilter, filterLinks));
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

        for (Filter f : filters) {
            this.filtersLR.add(SymbolicFilter.genSymbolicFilterFromTableNode(rt, f));
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
    private SymbolicFilter promoteLeftFilter(SymbolicFilter sf1) {
        Set<Integer> promotedFilter = new HashSet<>();
        for (int i : sf1.getFilterRep()) {
            for (int j  = 0; j < this.st2.getBaseTable().getContent().size(); j ++) {
                promotedFilter.add(i * this.st2.getBaseTable().getContent().size() + j);
            }
        }
        return new SymbolicFilter(promotedFilter, this.getBaseTable().getContent().size());
    }

    // Promote a filter in right table to a filter for current baseTable
    private SymbolicFilter promoteRightFilter(SymbolicFilter sf2) {
        Set<Integer> promotedFilter = new HashSet<>();
        for (int i = 0; i < st1.getBaseTable().getContent().size(); i ++) {
            for (int j : sf2.getFilterRep()) {
                promotedFilter.add(i * this.st2.getBaseTable().getContent().size() + j);
            }
        }
        return new SymbolicFilter(promotedFilter, this.getBaseTable().getContent().size());
    }

    private SymbolicFilter demoteToLeftFilter(SymbolicFilter f) {
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
        return new SymbolicFilter(demotedFilter, st1.getBaseTable().getContent().size());
    }

    private SymbolicFilter demoteToRightFilter(SymbolicFilter f) {
        Set<Integer> demotedFilter = new HashSet<>();

        for (Integer n : f.getFilterRep()) {
            demotedFilter.add(n % st2.getBaseTable().getContent().size());
        }
        return new SymbolicFilter(demotedFilter, st2.getBaseTable().getContent().size());
    }


    public List<Filter> decodeLR(Pair<AbstractSymbolicTable, SymbolicFilter> sfp, EnumContext ec) {

        List<Filter> filters = EnumCanonicalFilters
                .enumCanonicalFilterJoinNode(this.representitiveTableNode, ec);

        List<Filter> result = new ArrayList<Filter>();

        for (Filter f : filters) {
            try {
                if (SymbolicFilter
                        .genSymbolicFilter(this.representitiveTableNode.eval(new Environment()), f)
                        .equals(sfp.getValue()))
                    result.add(f);
            } catch (SQLEvalException e) {
                e.printStackTrace();
                continue;
            }
        }

        return result;
    }

    public List<List<Filter>> decodeLR(Set<Pair<AbstractSymbolicTable, SymbolicFilter>> srcs, EnumContext ec) {
        List<List<Filter>> unRotatedFilters = new ArrayList<>();
        for (Pair<AbstractSymbolicTable, SymbolicFilter> p : srcs) {
            if (p.getKey().equals(this)) {
                unRotatedFilters.add(decodeLR(p, ec));
            }
        }
        return CombinationGenerator.rotateList(unRotatedFilters);
    }

    // decoding the filter representation based on the links between filters
    @Override
    public List<TableNode> decodeToQuery(Pair<AbstractSymbolicTable, SymbolicFilter> sfp, EnumContext ec, FilterLinks fl) {

        assert (sfp.getKey().equals(this));

        TableNode coreTableNode = this.queryForBaseTable(ec);

        List<TableNode> result = new ArrayList<>();

        // this means that the query can be obtained by only applying a filter to it
        List<Filter> directFilter = this.decodeLR(sfp, ec);

        for (Filter f : directFilter) {
            List<ValNode> vals = coreTableNode.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());
            // rename the filters so that the filters refer to the elements in the new table.
            ValNodeSubstBinding vsb = new ValNodeSubstBinding();
            for (int i = 0; i < coreTableNode.getSchema().size(); i ++) {
                vsb.addBinding(new Pair<>(
                        new NamedVal(this.representitiveTableNode.getSchema().get(i)),
                        new NamedVal(coreTableNode.getSchema().get(i))));
            }
            TableNode tn = new SelectNode(vals, coreTableNode, f.substNamedVal(vsb));
            result.add(tn);
        }

        Set<Set<Pair<AbstractSymbolicTable, SymbolicFilter>>> srcSet = fl.retrieveSource(sfp);
        if (srcSet == null)
            return result;

        // the followings are the queries with several leading nodes
        for (Set<Pair<AbstractSymbolicTable, SymbolicFilter>> s : srcSet) {

            List<List<Filter>> filterList = this.decodeLR(s, ec);

            // now try to decode left and right table
            List<TableNode> decodeSt1 = new ArrayList<>(), decodeSt2 = new ArrayList<>();
            for (Pair<AbstractSymbolicTable, SymbolicFilter> p : s) {
                if (p.getKey().equals(this.st1)) {
                    decodeSt1 = st1.decodeToQuery(p, ec, fl);
                }
                if (p.getKey().equals(this.st2)) {
                    decodeSt2 = st2.decodeToQuery(p, ec, fl);
                }
            }
            List<Filter> LRFilters = filterList.stream()
                    .map(lst -> LogicAndFilter.connectByAnd(lst)).collect(Collectors.toList());

            for (TableNode tn1 : decodeSt1) {
                for (TableNode tn2 : decodeSt2) {
                    JoinNode jn = new JoinNode(Arrays.asList(tn1, tn2));
                    RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
                    for (Filter f : LRFilters) {
                        ValNodeSubstBinding vsb = new ValNodeSubstBinding();

                        for (int i = 0; i < this.representitiveTableNode.getSchema().size(); i ++) {
                            vsb.addBinding(new Pair<>(
                                    new NamedVal(this.representitiveTableNode.getSchema().get(i)),
                                    new NamedVal(rt.getSchema().get(i))));
                        }
                        List<ValNode> vals = rt.getSchema().stream()
                                .map(x -> new NamedVal(x))
                                .collect(Collectors.toList());
                        TableNode tn = new SelectNode(vals, rt, f.substNamedVal(vsb));
                        result.add(tn);
                    }
                }
            }
        }

        return result;
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
}
