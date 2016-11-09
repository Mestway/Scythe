package summarytable;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.FilterEnumerator;
import global.GlobalConfig;
import global.Statistics;
import backward_inference.CellToCellMap;
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

    // this field records the base table and the query that construct the base table
    private Table baseTable;
    private TableNode baseTableSrc;

    private boolean allowDisjunctiveFilter = false;

    // The children node of the baseTableSrc, indicating how baseTableSrc is constructed
    // e.g. 1) if the baseTableSrc is a named table from user input, the srcTreeChildren is empty,
    //              and the baseTableSrc is already the final query for baseTable.
    //      2) if the baseTableSrc is an aggregation node,
    //             the srcTreeChildren indicates how the coreTable of the baseTable is constructed.
    private List<Pair<AbstractSummaryTable, BVFilter>> baseTableSrcChildren = new ArrayList<>();

    private Set<BVFilter> primitiveBVFilters = new HashSet<>();
    // mapping each symbolic filter to its cost and concrete form
    private Map<BVFilter, Pair<Double, List<Filter>>> decodedPrimitives = new HashMap<>();

    // after primitiveFiltersEvaluated is done, the list will be ready to be used.
    private List<BVFilter> primitives = new ArrayList<>();
    private boolean primitiveFiltersEvaluated = false;

    public PrimitiveSummaryTable(Table baseTable, TableNode baseTableSrc, boolean allowDisj) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        // the symbolic primitive filters are not evaluated here because we want to keep them lazy
        this.primitiveBVFilters = new HashSet<>();

        // set that disjunctions are allowed for the named table
        this.allowDisjunctiveFilter = allowDisj;
    }

    public PrimitiveSummaryTable(Table baseTable,
                                 TableNode baseTableSrc,
                                 List<Pair<AbstractSummaryTable, BVFilter>> pairs) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        // the symbolic primitive filters are not evaluated here because we want to keep them lazy
        this.primitiveBVFilters = new HashSet<>();

        // this constructor represents how this summary table is constructed from
        baseTableSrcChildren.addAll(pairs);
    }

    @Override
    public List<Table> getAllPrimitiveBaseTables() {
        List<Table> result = new ArrayList<>();
        result.add(this.getBaseTable());
        return result;
    }

    @Override
    public Table getBaseTable() { return this.baseTable; }
    public Class getBaseTableSrcNatureClass() {
        if (this.baseTableSrc instanceof NamedTable) {
            return NamedTable.class;
        } else {
            return ((RenameTableNode) baseTableSrc).getTableNode().getClass();
        }
    }

    public void setBaseTable(Table baseTable) { this.baseTable = baseTable; }

    public Set<BVFilter> getSymbolicFilters() { return this.primitiveBVFilters; }

    public void setSymbolicFilters(Set<BVFilter> filters) {
        this.primitiveBVFilters = filters;
        this.primitiveBVFilters.add(BVFilter.genSymbolicFilter(this.baseTable, new EmptyFilter()));
    }

    @Override
    public void emitFinalVisitAllTables(
            MappingInference mi,
            EnumContext ec,
            BiConsumer<AbstractSummaryTable, BVFilter> f) {

        List<List<Set<Integer>>> rowMappingRangeInstances =
                mi.genConcreteColMappings()
                .stream()
                .map(l -> mi.genRowMappingRange(l))
                .collect(Collectors.toList());

        this.encodePrimitiveFilters(ec);
        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        int noneBogusFilterCount = 0;
        int targetFilterSize =  primitives.size() *  primitives.size() / 2 + 1;

        // this is the traditional way, invoking the mergeAndLink function to get it.
        for (int i = 0; i < primitives.size(); i ++) {
            // we can prune this if we don't allow disjunctive filters
            if (! allowDisjunctiveFilter && ! fullyContainedARange(primitives.get(i), rowMappingRangeInstances))
                continue;
            for (int j = i + 1; j < primitives.size(); j ++) {

                // Consider conjunctions on the table
                BVFilter conjFilter = BVFilter.mergeFilterConj(
                        primitives.get(i),
                        primitives.get(j));

                if (rowMappingRangeInstances
                        .stream()
                        .map(ls -> conjFilter.exactMatchRange(ls))
                        .reduce((x,y)->x || y).get()) {
                    noneBogusFilterCount ++;
                    f.accept(this, conjFilter);
                }

                if (this.allowDisjunctiveFilter) {
                    BVFilter disjFilter = BVFilter.mergeFilterDisj(
                            primitives.get(i),
                            primitives.get(j));

                    if (rowMappingRangeInstances
                            .stream()
                            .map(ls -> conjFilter.exactMatchRange(ls))
                            .reduce((x,y)->x || y).get()) {
                        noneBogusFilterCount ++;
                        f.accept(this, conjFilter);
                    }
                }
            }
        }

        BVFilter emptyFilter = BVFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter());

        if (rowMappingRangeInstances
                .stream()
                .map(ls -> emptyFilter.exactMatchRange(ls))
                .reduce((x,y)->x || y).get()) {
            noneBogusFilterCount ++;
            f.accept(this, emptyFilter);
        }

        Statistics.sum_back_filter_bogus_rate += ((float)(targetFilterSize - noneBogusFilterCount)) / targetFilterSize;
        Statistics.back_filter_bogus_cases ++;
    }

    // give the maximum filter length,
    // instantiate all possible tables that can be generated from filtering the current table.
    // Currently the max-length is fixed as 2
    @Override
    public Set<BVFilter> instantiateAllFilters() {

        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        Set<BVFilter> result = AbstractSummaryTable.genConjunctiveFilters(this,
                this.primitiveBVFilters.stream().collect(Collectors.toList()));

        // alternative disjunctive filters
        if (this.allowDisjunctiveFilter) {
            result.addAll(AbstractSummaryTable.genDisjunctiveFilters(this,
                    this.primitiveBVFilters.stream().collect(Collectors.toList())));
        }

        return result;
    }

    public List<Filter> decodePrimitiveFilter(BVFilter sf, TableNode tn) {
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
        return this.primitiveBVFilters.size();
    }

    // calculate what are the primitive filters for this symbolic table.
    @Override
    void encodePrimitiveFilters(EnumContext ec) {

        if (this.primitiveFiltersEvaluated) return;

        // only simple filter will be encoded into bit-vectors
        int backUpMaxFilterLength = ec.getMaxFilterLength();
        ec.setMaxFilterLength(1);

        List<Filter> filters;

        if (this.baseTableSrc instanceof RenameTableNode
                && ((RenameTableNode) this.baseTableSrc).getTableNode() instanceof AggregationNode) {
            // filters = EnumCanonicalFilters.enumCanonicalFilterNamedTable(new NamedTable(this.baseTable), ec);
            filters = FilterEnumerator.enumCanonicalFilterAggrNode((RenameTableNode) this.baseTableSrc, ec);
        } else {
            // this is for left-join nodes and named tables
            filters = FilterEnumerator.enumCanonicalFilterNamedTable(new NamedTable(this.baseTable), ec);
        }
        ec.setMaxFilterLength(backUpMaxFilterLength);

        // the empty filter is added here to make it complete.
        filters.add(new EmptyFilter());

        // add possible decoding result for each symbolic table
        decodedPrimitives.put(BVFilter.genSymbolicFilter(baseTable, new EmptyFilter()), new Pair<>(0., new ArrayList<>()));

        Set<BVFilter> symfilters = new HashSet<>();
        for (Filter f : filters) {
            BVFilter symFilter = BVFilter.genSymbolicFilter(baseTable, f);
            symfilters.add(symFilter);
            if (! decodedPrimitives.containsKey(symFilter)) {
                decodedPrimitives.put(symFilter, new Pair<>(99999., new ArrayList<>()));
            }

            // retrieve the entry of the filter and update it
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

        this.primitiveBVFilters = symfilters;
        this.primitiveFiltersEvaluated = true;
        this.primitives = this.primitiveBVFilters.stream().collect(Collectors.toList());

        // calculating count
        this.primitiveSynFilterCount = filters.size();
        this.primitiveBitVecFilterCount = this.primitives.size();

        // calculating the reduction rate from bit vector to syntax filters
        // System.out.println("#(BitVec)/#(SynFilter): " + this.primitives.size() + " / " + filters.size() + " = " + (this.primitives.size() / ((float)filters.size())));

        Statistics.sum_red_syn_to_bv += ((float)filters.size()) / (this.primitives.size());
        Statistics.sum_red_syn_to_bv ++;


        if (GlobalConfig.STAT_MODE) {
            Set<BVFilter> conj = AbstractSummaryTable.genConjunctiveFilters(this, this.primitiveBVFilters.stream().collect(Collectors.toList()));
            Set<BVFilter> disj = AbstractSummaryTable.genDisjunctiveFilters(this, this.primitiveBVFilters.stream().collect(Collectors.toList()));
            conj.addAll(disj);
            System.out.println("[Filter Reduction Rate] " + ((double) (filters.size() * filters.size() * 2 + filters.size())) / conj.size());
        }
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

        if (allowDisjunctiveFilter) {
            // in this case we cannot prune
            validFilters = this.primitives;
        }

        // this part deals only with none-empty filters
        for (int i = 0; i < validFilters.size(); i ++) {
            BVFilter pi = validFilters.get(i);
            for (int j = i + 1; j < validFilters.size(); j ++) {
                BVFilter pj = validFilters.get(j);
                BVFilter mergedij = BVFilter.mergeFilterConj(pi, pj);

                if (targets.contains(mergedij)) {
                    Set<BVFilter> s = new HashSet<>();
                    s.add(pi); s.add(pj);
                    BVFilterCompTree sct = new BVFilterCompTree(this, s);

                    result.get(mergedij).add(sct);
                }

                if (this.allowDisjunctiveFilter) {
                    BVFilter disjij = BVFilter.mergeFilterDisj(pi, pj);

                    if (targets.contains(disjij)) {
                        Set<BVFilter> s = new HashSet<>();
                        s.add(pi); s.add(pj);
                        BVFilterCompTree sct = new BVFilterCompTree(this, s, true);

                        result.get(disjij).add(sct);
                    }
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

        if (this.baseTableSrc instanceof NamedTable) {
            // In this case, the table is a table from input example
            return Arrays.asList(this.baseTableSrc);
        } else if (this.baseTableSrc instanceof RenameTableNode
                && ((RenameTableNode) this.baseTableSrc).getTableNode() instanceof AggregationNode) {

            // In this case, the table src is an aggregation node
            AbstractSummaryTable symTable = baseTableSrcChildren.get(0).getKey();
            BVFilter sf = baseTableSrcChildren.get(0).getValue();

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
        } else if (this.baseTableSrc instanceof RenameTableNode
                && ((RenameTableNode) this.baseTableSrc).getTableNode() instanceof LeftJoinNode) {

            List<TableNode> tableSrcs = new ArrayList<>();

            List<List<TableNode>> coresList = baseTableSrcChildren
                    .stream()
                    .map(p -> {
                        AbstractSummaryTable symTable = p.getKey();
                        BVFilter sf = p.getValue();
                        Set<BVFilter> sfSet = new HashSet<>();
                        sfSet.add(sf);
                        List<BVFilterCompTree> trees = symTable.batchGenDecomposition(sfSet).get(sf);
                        List<TableNode> cores = new ArrayList<>();
                        for (BVFilterCompTree tr : trees) {
                            cores.addAll(tr.translateToTopSQL(ec));
                        }
                        return cores;
                    }).collect(Collectors.toList());

            List<TableNode> leftCores = coresList.get(0);
            List<TableNode> rightCores = coresList.get(1);

            for (TableNode lCore : leftCores) {
                for (TableNode rCore : rightCores) {
                    TableNode rt = RenameTNWrapper.tryRename(
                            ((LeftJoinNode) ((RenameTableNode) this.baseTableSrc).getTableNode())
                                    .substCoreTable(lCore, rCore));
                    tableSrcs.add(rt);
                }
            }

            return tableSrcs;
        }

        System.err.println("[ERROR@PrimitiveSummaryTable438] None implemented decoding process");
        return null;
    }

    @Override
    public double estimatePrimitiveSymFilterCost(BVFilter sf) {
        return this.decodedPrimitives.get(sf).getKey();
    }

    @Override
    public List<NamedTable> namedTableInvolved() {
        return this.baseTableSrc.namedTableInvolved();
    }

}
