package symbolic;

import enumerator.context.EnumContext;
import enumerator.primitive.EnumCanonicalFilters;
import global.Statistics;
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

import java.util.*;
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

        System.out.println("Filters to be visited " + filtersToBeVisited);

        int storedCount = 0;

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

                    storedCount ++;
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
        int filtersToBeVisited = promotedFilters1.keySet().size() * promotedFilters2.keySet().size() * lrFiltersLinks.getKey().size();
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
        Statistics.sum_last_stage_red_visited_compound += filtersToBeVisited / ((float)tt);
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
        this.primitives = this.filtersLR.stream().collect(Collectors.toList());

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
}
