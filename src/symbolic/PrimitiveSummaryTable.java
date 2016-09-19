package symbolic;

import forward_enumeration.primitive.EnumCanonicalFilters;
import forward_enumeration.context.EnumContext;
import global.Statistics;
import backward_inference.CoordInstMap;
import backward_inference.MappingInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.CostEstimator;
import util.Pair;
import util.RenameTNWrapper;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * SymbolicTable
 * Created by clwang on 3/26/16.
 */
public class PrimitiveSummaryTable extends AbstractSummaryTable {

    // this field records what is the query to construct the base table
    private TableNode baseTableSrc;

    // This field only presence when the baseTableSrc is an aggregation node,
    // the aggregation core identifies the core used in enumerating aggregation.
    private Optional<Pair<AbstractSummaryTable, BVFilter>> coreSymTableNFilter;

    private Table baseTable;

    private Set<BVFilter> symbolicPrimitiveFilters = new HashSet<>();
    // mapping each symbolic filter to its cost and concrete form
    private Map<BVFilter, Pair<Double, List<Filter>>> decodedPrimitives = new HashMap<>();

    // after primitveFiltersEvaluated is done, the list will be ready to be used.
    private List<BVFilter> primitives = new ArrayList<>();
    private boolean primitiveFiltersEvaluated = false;

    public TableNode getBaseTableSrc() { return this.baseTableSrc; }

    public PrimitiveSummaryTable(Table baseTable, TableNode baseTableSrc) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        // the symbolic primitive filters are not evaluated here because we want to keep them lazy
        this.symbolicPrimitiveFilters = new HashSet<>();
        this.coreSymTableNFilter = Optional.ofNullable(null);
    }

    public PrimitiveSummaryTable(Table baseTable, TableNode baseTableSrc, Pair<AbstractSummaryTable, BVFilter> p) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        // the symbolic primitive filters are not evaluated here because we want to keep them lazy
        this.symbolicPrimitiveFilters = new HashSet<>();
        this.coreSymTableNFilter = Optional.of(p);
    }

    public PrimitiveSummaryTable(Table baseTable, TableNode baseTableSrc, EnumContext ec) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        this.symbolicPrimitiveFilters = new HashSet<>();

        // only simple filter will be evaluated
        int backUpMaxFilterLength = ec.getMaxFilterLength();
        ec.setMaxFilterLength(1);

        List<Filter>  filters;
        if (this.baseTableSrc instanceof RenameTableNode
                && ((RenameTableNode) this.baseTableSrc).getTableNode() instanceof AggregationNode)
            filters = EnumCanonicalFilters.enumCanonicalFilterAggrNode((RenameTableNode) baseTableSrc, ec);
        else
            filters = EnumCanonicalFilters.enumCanonicalFilterNamedTable(new NamedTable(this.baseTable), ec);

        ec.setMaxFilterLength(backUpMaxFilterLength);

        // the empty filter is added here to make it complete.
        filters.add(new EmptyFilter());

        Set<BVFilter> symfilters = new HashSet<>();
        for (Filter f : filters) {
            symfilters.add(BVFilter.genSymbolicFilter(this.baseTable, f));
        }
        this.symbolicPrimitiveFilters = symfilters;
        this.primitiveFiltersEvaluated = true;
        this.primitives = this.symbolicPrimitiveFilters.stream().collect(Collectors.toList());

        // calculating count
        this.primitiveSynFilterCount = filters.size();
        this.primitiveBitVecFilterCount = this.primitives.size();
    }

    @Override
    public List<Table> getAllPrimitiveBaseTables() {
        List<Table> result = new ArrayList<>();
        result.add(this.getBaseTable());
        return result;
    }

    @Override
    public Table getBaseTable() { return this.baseTable; }

    public void setBaseTable(Table baseTable) { this.baseTable = baseTable; }

    public Set<BVFilter> getSymbolicFilters() { return this.symbolicPrimitiveFilters; }

    public void setSymbolicFilters(Set<BVFilter> filters) {
        this.symbolicPrimitiveFilters = filters;
        this.symbolicPrimitiveFilters.add(BVFilter.genSymbolicFilter(this.baseTable, new EmptyFilter()));
    }

    public Set<Pair<BVFilter, BVFilter>> visitDemotedSpace(
            EnumContext ec, Set<BVFilter> demotedExtFilters) {

        MappingInference mi = MappingInference.buildMapping(this.getBaseTable(), ec.getOutputTable());
        List<CoordInstMap> map = mi.genMappingInstances();

        // evaluate the set of all target filters that we learnt from output
        Set<BVFilter> targetFilters = new HashSet<>();
        for (CoordInstMap cim : map) {
            BVFilter sf = new BVFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        // this is the traditional way, invoking the mergeAndLink function to get it.
        Set<BVFilter> allFiltersLinks = AbstractSummaryTable.mergeAndLinkFilters(this,
                this.symbolicPrimitiveFilters.stream().collect(Collectors.toList()));

        List<BVFilter> validFilter = new ArrayList<>();
        for (BVFilter sf : allFiltersLinks) {
            if (! fullyContainedAnElement(sf, targetFilters))
                continue;
            validFilter.add(sf);
        }

        // TODO: we assume that all filters send in are those already checked,
        // TODO: i.e. all of them should contain at least one element in the target filter set

        Set<Pair<BVFilter, BVFilter>> validPairs = new HashSet<>();

        for (BVFilter vf : validFilter) {
            for (BVFilter ef : demotedExtFilters) {
                BVFilter merged = BVFilter.mergeFilter(vf, ef, AbstractSummaryTable.mergeFunction);
                if (targetFilters.contains(merged)) {
                    validPairs.add(new Pair<>(vf, ef));
                }
            }
        }

        return validPairs;
    }

    @Override
    public void emitFinalVisitAllTables(
            MappingInference mi,
            EnumContext ec,
            BiConsumer<AbstractSummaryTable, BVFilter> f) {

        List<CoordInstMap> map = mi.genMappingInstances();

        // evaluate the set of all target filters that we learnt from output
        Set<BVFilter> targetFilters = new HashSet<>();
        for (CoordInstMap cim : map) {
            BVFilter sf = new BVFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        this.evalPrimitive(ec);
        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        // this is the traditional way, invoking the mergeAndLink function to get it.
        Set<BVFilter> allFilters = AbstractSummaryTable.mergeAndLinkFilters(this,
                this.symbolicPrimitiveFilters.stream().collect(Collectors.toList()));

        int noneBogusFilterCount = 0;

        for (BVFilter sf : allFilters) {
            if (targetFilters.contains(sf)) {
                noneBogusFilterCount ++;
                f.accept(this, sf);
            }
        }

        Statistics.sum_back_filter_bogus_rate += ((float)(targetFilters.size() - noneBogusFilterCount)) / targetFilters.size();
        Statistics.back_filter_bogus_cases ++;
    }

    // give the maximum filter length,
    // instantiate all possible tables that can be generated from filtering the current table.
    // Currently the max-length is fixed as 2
    @Override
    public Set<BVFilter> instantiateAllFilters() {
        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        // this is the traditional way, invoking the mergeAndLink function to get it.
        return AbstractSummaryTable.mergeAndLinkFilters(this,
                this.symbolicPrimitiveFilters.stream().collect(Collectors.toList()));

        // This is an alternative way, invoking the lazy enumerator
        /* Set<SymbolicFilter> result = new HashSet<>();
        FilterLinks fl = new FilterLinks();

        int k = 0;
        while (true) {
            Optional<Pair<SymbolicFilter, FilterLinks>> op = lazyFilterEval(k);
            k ++;
            if (! op.isPresent()) break;

            result.add(op.get().getKey());
            fl = FilterLinks.merge(Arrays.asList(fl, op.get().getValue()));
        }

        // this is used to make sure that the empty filter is added
        // result.add(SymbolicFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter()));

        this.allfiltersEnumerated = true;
        this.totalBitVecFiltersCount = result.size();
        return new Pair<>(result, fl); */
    }

    // A lazy version of instantiating all filters
    @Override
    public Optional<BVFilter> lazyFilterEval(Integer index) {

        assert primitiveFiltersEvaluated;

        if (index == 0)
            return Optional.of(BVFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter()));

        index --;

        Pair<Integer, Integer> p = this.inverseFilterIndex(this.getPrimitiveFilterNum(), index);

        if ((p.getKey() == -1) && (p.getValue() == -1))
            return Optional.empty();

        BVFilter mergedFilter = BVFilter.mergeFilter(
                primitives.get(p.getKey()),
                primitives.get(p.getValue()),
                AbstractSummaryTable.mergeFunction);

        // add the link from two source filter to the merged filter itself
        Set<Pair<AbstractSummaryTable, BVFilter>> srcs = new HashSet<>();
        srcs.add(new Pair<>(this, primitives.get(p.getKey())));
        srcs.add(new Pair<>(this, primitives.get(p.getValue())));

        return Optional.of(mergedFilter);
    }

    public List<Filter> decodePrimitiveFilter(BVFilter sf, TableNode tn, EnumContext ec) {
        // the table can either be a named table or a renamed table from an aggregation table.

        try {
            if (sf.getFilterRep().size() == tn.eval(new Environment()).getContent().size())
                return Arrays.asList(new EmptyFilter());
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }

        ValNodeSubstBinding vnsb = new ValNodeSubstBinding();
        for (int i = 0; i < this.baseTableSrc.getSchema().size(); i++) {
            vnsb.addBinding(new Pair<>(new NamedVal(this.baseTableSrc.getSchema().get(i)), new NamedVal(tn.getSchema().get(i))));
        }
        List<Filter> result = decodedPrimitives.get(sf).getValue();

        Statistics.sumPrimitiveFilterCount += result.size();
        Statistics.cntPrimitiveFilterCount ++;
        Statistics.maxPrimitiveFilterCount = Statistics.maxPrimitiveFilterCount > result.size() ?  Statistics.maxPrimitiveFilterCount : result.size();

        List<Filter> postProcessed = new ArrayList<>();
        // TODO: if we want to limit the number, use the commented one
        //        for (int i = 0; i < 5; i ++) {
        for (int i = 0; i < result.size(); i ++) {
            if (i == result.size()) break;
            postProcessed.add(result.get(i).substNamedVal(vnsb));
        }
        return postProcessed;
    }

    @Override
    public int getPrimitiveFilterNum() {
        return this.symbolicPrimitiveFilters.size();
    }

    // calculate what are the primitive filters for this symbolic table.
    @Override
    void evalPrimitive(EnumContext ec) {

        if (this.primitiveFiltersEvaluated) return;

        // only simple filter will be evaluated
        int backUpMaxFilterLength = ec.getMaxFilterLength();
        ec.setMaxFilterLength(1);

        List<Filter> filters;

        if (this.baseTableSrc instanceof RenameTableNode
                && ((RenameTableNode) this.baseTableSrc).getTableNode() instanceof AggregationNode) {
            // filters = EnumCanonicalFilters.enumCanonicalFilterNamedTable(new NamedTable(this.baseTable), ec);
            filters = EnumCanonicalFilters.enumCanonicalFilterAggrNode((RenameTableNode) this.baseTableSrc, ec);
        } else {
            filters = EnumCanonicalFilters.enumCanonicalFilterNamedTable(new NamedTable(this.baseTable), ec);
        }
        ec.setMaxFilterLength(backUpMaxFilterLength);

        // the empty filter is added here to make it complete.
        filters.add(new EmptyFilter());

        decodedPrimitives.put(BVFilter.genSymbolicFilter(baseTable, new EmptyFilter()), new Pair<>(0., new ArrayList<>()));

        Set<BVFilter> symfilters = new HashSet<>();
        for (Filter f : filters) {
            BVFilter symFilter = BVFilter.genSymbolicFilter(baseTable, f);
            symfilters.add(symFilter);
            if (! decodedPrimitives.containsKey(symFilter)) {
                decodedPrimitives.put(symFilter, new Pair<>(99999., new ArrayList<>()));
            }

            Pair<Double, List<Filter>> entryRef = decodedPrimitives.get(symFilter);

            entryRef.getValue().add(f);
            double cost = CostEstimator.estimateFilterCost(f,
                    TableNode.nameToOriginMap(
                            baseTableSrc.getSchema(),
                            baseTableSrc.originalColumnName()));
            if (cost < entryRef.getKey()) {
                decodedPrimitives.put(symFilter, new Pair<>(cost, entryRef.getValue()));
            }
        }

        for (Map.Entry<BVFilter, Pair<Double, List<Filter>>> e : decodedPrimitives.entrySet()) {
            e.getValue().getValue().sort(new Comparator<Filter>() {
                @Override
                public int compare(Filter o1, Filter o2) {
                    double cost1 = CostEstimator.estimateFilterCost(o1,
                            TableNode.nameToOriginMap(baseTableSrc.getSchema(), baseTableSrc.originalColumnName()));
                    double cost2 = CostEstimator.estimateFilterCost(o2,
                            TableNode.nameToOriginMap(baseTableSrc.getSchema(), baseTableSrc.originalColumnName()));
                    return cost1 <= cost2 ? (cost1 < cost2 ? -1 : 0) : 1;
                }
            });
        }

        this.symbolicPrimitiveFilters = symfilters;
        this.primitiveFiltersEvaluated = true;
        this.primitives = this.symbolicPrimitiveFilters.stream().collect(Collectors.toList());

        // calculating count
        this.primitiveSynFilterCount = filters.size();
        this.primitiveBitVecFilterCount = this.primitives.size();

        // calculating the reduction rate from bit vector to syntax filters
        // System.out.println("#(BitVec)/#(SynFilter): " + this.primitives.size() + " / " + filters.size() + " = " + (this.primitives.size() / ((float)filters.size())));

        Statistics.sum_red_syn_to_bv += ((float)filters.size()) / (this.primitives.size());
        Statistics.sum_red_syn_to_bv ++;

    }

    @Override
    public TableNode queryForBaseTable(EnumContext ec) {
        return this.baseTableSrc;
    }

    @Override
    public int compoundPrimitiveFilterCount() {
        int n = this.getPrimitiveFilterNum();
        return n * (n - 1) / 2 + 1;
    }

    @Override
    public int compoundFilterCount() {
        int n = this.getPrimitiveFilterNum();
        return n * (n - 1) / 2 + 1;
    }

    @Override
    public List<Integer> getTableRightIndexBoundries() {
        List<Integer> result = new ArrayList<>();
        result.add(this.getBaseTable().getSchema().size());
        return result;
    }

    @Override
    public Map<BVFilter, List<BVFilterCompTree>> batchGenDecomposition(Set<BVFilter> targets) {

        Map<BVFilter, List<BVFilterCompTree>> result = new HashMap<>();
        for (BVFilter sf : targets) {
            result.put(sf, new ArrayList<>());
        }
        List<BVFilter> validFilters = this.primitives.stream()
                .filter(x -> fullyContainedAnElement(x, targets))
                .collect(Collectors.toList());

        // this part deals only with none-empty filters
        for (int i = 0; i < validFilters.size(); i ++) {
            BVFilter pi = validFilters.get(i);
            for (int j = i + 1; j < validFilters.size(); j ++) {
                BVFilter pj = validFilters.get(j);
                BVFilter mergedij = BVFilter.mergeFilter(pi, pj, AbstractSummaryTable.mergeFunction);

                if (targets.contains(mergedij)) {
                    Set<BVFilter> s = new HashSet<>();
                    s.add(pi); s.add(pj);
                    BVFilterCompTree sct = new BVFilterCompTree(this, s);

                    result.get(mergedij).add(sct);
                }
            }
        }

        BVFilter emptyFilter = BVFilter.genSymbolicFilter(this.baseTable, new EmptyFilter());
        if (targets.contains(emptyFilter)) {
            Set<BVFilter> s = new HashSet<>();
            s.add(emptyFilter);
            BVFilterCompTree sct = new BVFilterCompTree(this, s);

            result.get(emptyFilter).add(sct);
        }


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
                        return cost1 < cost2 ?  -1 : (cost1 == cost2 ? 0 : 1);
                    }
                });
                trees = trees.subList(0, 5);
                postProcessed.put(i.getKey(), trees);
            } else {
                postProcessed.put(i.getKey(), i.getValue());
            }
        }

        return postProcessed;
    }

    public List<TableNode> genTableSrc(EnumContext ec) {

        if (! this.coreSymTableNFilter.isPresent())
            return Arrays.asList(this.baseTableSrc);

        AbstractSummaryTable symTable = coreSymTableNFilter.get().getKey();
        BVFilter sf = coreSymTableNFilter.get().getValue();


        Set<BVFilter> sfSet = new HashSet<>();
        sfSet.add(sf);
        List<BVFilterCompTree> trees = symTable.batchGenDecomposition(sfSet).get(sf);
        List<TableNode> cores = new ArrayList<>();
        for (BVFilterCompTree tr : trees) {
            cores.addAll(tr.translateToTopSQL(ec));
        }

        List<TableNode> tableSrcs = new ArrayList<>();
        for(TableNode core : cores) {
            tableSrcs.add(RenameTNWrapper.tryRename(((AggregationNode)((RenameTableNode)baseTableSrc).getTableNode()).substCoreTable(core)));
        }

        return tableSrcs;
    }

    @Override
    public double estimatePrimitiveSymFilterCost(BVFilter sf) {
        return this.decodedPrimitives.get(sf).getKey();
    }

}
