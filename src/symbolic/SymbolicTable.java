package symbolic;

import enumerator.primitive.EnumCanonicalFilters;
import enumerator.context.EnumContext;
import global.Statistics;
import mapping.CoordInstMap;
import mapping.MappingInference;
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
public class SymbolicTable extends AbstractSymbolicTable {

    // this field records what is the query to construct the base table
    private TableNode baseTableSrc;

    // This field only presence when the baseTableSrc is an aggregation node,
    // the aggregation core identifies the core used in enumerating aggregation.
    private Optional<Pair<AbstractSymbolicTable, SymbolicFilter>> coreSymTableNFilter;

    private Table baseTable;

    private Set<SymbolicFilter> symbolicPrimitiveFilters = new HashSet<>();
    // mapping each symbolic filter to its cost and concrete form
    private Map<SymbolicFilter, Pair<Double, List<Filter>>> decodedPrimitives = new HashMap<>();

    // after primitveFiltersEvaluated is done, the list will be ready to be used.
    private List<SymbolicFilter> primitives = new ArrayList<>();
    private boolean primitiveFiltersEvaluated = false;

    public TableNode getBaseTableSrc() { return this.baseTableSrc; }

    public SymbolicTable(Table baseTable, TableNode baseTableSrc) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        // the symbolic primitive filters are not evaluated here because we want to keep them lazy
        this.symbolicPrimitiveFilters = new HashSet<>();
        this.coreSymTableNFilter = Optional.ofNullable(null);
    }

    public SymbolicTable(Table baseTable, TableNode baseTableSrc, Pair<AbstractSymbolicTable, SymbolicFilter> p) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        // the symbolic primitive filters are not evaluated here because we want to keep them lazy
        this.symbolicPrimitiveFilters = new HashSet<>();
        this.coreSymTableNFilter = Optional.of(p);
    }

    public SymbolicTable(Table baseTable, TableNode baseTableSrc, EnumContext ec) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        this.symbolicPrimitiveFilters = new HashSet<>();

        // only simple filter will be evaluated
        int backUpMaxFilterLength = ec.getMaxFilterLength();
        ec.setMaxFilterLength(1);

        List<Filter>  filters = new ArrayList<>();
        if (this.baseTableSrc instanceof RenameTableNode
                && ((RenameTableNode) this.baseTableSrc).getTableNode() instanceof AggregationNode)
            filters = EnumCanonicalFilters.enumCanonicalFilterAggrNode((RenameTableNode) baseTableSrc, ec);
        else
            filters = EnumCanonicalFilters.enumCanonicalFilterNamedTable(new NamedTable(this.baseTable), ec);

        ec.setMaxFilterLength(backUpMaxFilterLength);

        // the empty filter is added here to make it complete.
        filters.add(new EmptyFilter());

        Set<symbolic.SymbolicFilter> symfilters = new HashSet<>();
        for (Filter f : filters) {
            symfilters.add(symbolic.SymbolicFilter.genSymbolicFilter(this.baseTable, f));
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

    public Set<SymbolicFilter> getSymbolicFilters() { return this.symbolicPrimitiveFilters; }

    public void setSymbolicFilters(Set<SymbolicFilter> filters) {
        this.symbolicPrimitiveFilters = filters;
        this.symbolicPrimitiveFilters.add(SymbolicFilter.genSymbolicFilter(this.baseTable, new EmptyFilter()));
    }

    public Set<Pair<SymbolicFilter, SymbolicFilter>> visitDemotedSpace(
            EnumContext ec, Set<SymbolicFilter> demotedExtFilters) {

        MappingInference mi = MappingInference.buildMapping(this.getBaseTable(), ec.getOutputTable());
        List<CoordInstMap> map = mi.genMappingInstances();

        // evaluate the set of all target filters that we learnt from output
        Set<SymbolicFilter> targetFilters = new HashSet<>();
        for (CoordInstMap cim : map) {
            SymbolicFilter sf = new SymbolicFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        // this is the traditional way, invoking the mergeAndLink function to get it.
        Pair<Set<SymbolicFilter>, FilterLinks> allFiltersLinks = AbstractSymbolicTable.mergeAndLinkFilters(this,
                this.symbolicPrimitiveFilters.stream().collect(Collectors.toList()));

        List<SymbolicFilter> validFilter = new ArrayList<>();
        for (SymbolicFilter sf : allFiltersLinks.getKey()) {
            if (! fullyContainedAnElement(sf, targetFilters))
                continue;
            validFilter.add(sf);
        }

        // TODO: we assume that all filters send in are those already checked,
        // TODO: i.e. all of them should contain at least one element in the target filter set

        Set<Pair<SymbolicFilter, SymbolicFilter>> validPairs = new HashSet<>();

        for (SymbolicFilter vf : validFilter) {
            for (SymbolicFilter ef : demotedExtFilters) {
                SymbolicFilter merged = SymbolicFilter.mergeFilter(vf, ef, AbstractSymbolicTable.mergeFunction);
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
            BiConsumer<AbstractSymbolicTable, SymbolicFilter> f) {

        List<CoordInstMap> map = mi.genMappingInstances();

        // evaluate the set of all target filters that we learnt from output
        Set<SymbolicFilter> targetFilters = new HashSet<>();
        for (CoordInstMap cim : map) {
            SymbolicFilter sf = new SymbolicFilter(cim.rowsInvolved(), this.getBaseTable().getContent().size());
            targetFilters.add(sf);
        }

        this.evalPrimitive(ec);
        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        // this is the traditional way, invoking the mergeAndLink function to get it.
        Pair<Set<SymbolicFilter>, FilterLinks> allFilters = AbstractSymbolicTable.mergeAndLinkFilters(this,
                this.symbolicPrimitiveFilters.stream().collect(Collectors.toList()));

        int noneBogusFilterCount = 0;

        for (SymbolicFilter sf : allFilters.getKey()) {
            if (targetFilters.contains(sf)) {
                noneBogusFilterCount ++;
                FilterLinks fl = new FilterLinks();
                fl.setLinkSource(allFilters.getValue().retrieveSource(new Pair<>(this, sf)), new Pair<>(this, sf));
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
    public Pair<Set<SymbolicFilter>, FilterLinks> instantiateAllFilters() {
        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        // this is the traditional way, invoking the mergeAndLink function to get it.
        return AbstractSymbolicTable.mergeAndLinkFilters(this,
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
    public Optional<Pair<SymbolicFilter, FilterLinks>> lazyFilterEval(Integer index) {

        assert primitiveFiltersEvaluated;

        if (index == 0)
            return Optional.of(new Pair<>(SymbolicFilter.genSymbolicFilter(this.getBaseTable(), new EmptyFilter()), new FilterLinks()));

        index --;

        FilterLinks filterLinks = new FilterLinks();
        Pair<Integer, Integer> p = this.inverseFilterIndex(this.getPrimitiveFilterNum(), index);

        if ((p.getKey() == -1) && (p.getValue() == -1))
            return Optional.empty();

        SymbolicFilter mergedFilter = SymbolicFilter.mergeFilter(
                primitives.get(p.getKey()),
                primitives.get(p.getValue()),
                AbstractSymbolicTable.mergeFunction);

        // add the link from two source filter to the merged filter itself
        Set<Pair<AbstractSymbolicTable, SymbolicFilter>> srcs = new HashSet<>();
        srcs.add(new Pair<>(this, primitives.get(p.getKey())));
        srcs.add(new Pair<>(this, primitives.get(p.getValue())));

        filterLinks.addLink(srcs, new Pair<>(this, mergedFilter));

        return Optional.of(new Pair<>(mergedFilter, filterLinks));
    }

    public List<Filter> decodePrimitiveFilter(SymbolicFilter sf, TableNode tn, EnumContext ec) {
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
        List<Filter> postProcessed = new ArrayList<>();
        for (int i = 0; i < 5; i ++) {
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

        Set<symbolic.SymbolicFilter> symfilters = new HashSet<>();
        for (Filter f : filters) {
            SymbolicFilter symFilter = SymbolicFilter.genSymbolicFilter(baseTable, f);
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

        for (Map.Entry<SymbolicFilter, Pair<Double, List<Filter>>> e : decodedPrimitives.entrySet()) {
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
        result.add(this.getBaseTable().getMetadata().size());
        return result;
    }

    @Override
    public Map<SymbolicFilter, List<SymFilterCompTree>> batchGenDecomposition(Set<SymbolicFilter> targets) {

        Map<SymbolicFilter, List<SymFilterCompTree>> result = new HashMap<>();
        for (SymbolicFilter sf : targets) {
            result.put(sf, new ArrayList<>());
        }
        List<SymbolicFilter> validFilters = this.primitives.stream()
                .filter(x -> fullyContainedAnElement(x, targets))
                .collect(Collectors.toList());

        // this part deals only with none-empty filters
        for (int i = 0; i < validFilters.size(); i ++) {
            SymbolicFilter pi = validFilters.get(i);
            for (int j = i + 1; j < validFilters.size(); j ++) {
                SymbolicFilter pj = validFilters.get(j);
                SymbolicFilter mergedij = SymbolicFilter.mergeFilter(pi, pj, AbstractSymbolicTable.mergeFunction);

                if (targets.contains(mergedij)) {
                    Set<SymbolicFilter> s = new HashSet<>();
                    s.add(pi); s.add(pj);
                    SymFilterCompTree sct = new SymFilterCompTree(this, s);

                    result.get(mergedij).add(sct);
                }
            }
        }

        SymbolicFilter emptyFilter = SymbolicFilter.genSymbolicFilter(this.baseTable, new EmptyFilter());
        if (targets.contains(emptyFilter)) {
            Set<SymbolicFilter> s = new HashSet<>();
            s.add(emptyFilter);
            SymFilterCompTree sct = new SymFilterCompTree(this, s);

            result.get(emptyFilter).add(sct);
        }


        Map<SymbolicFilter, List<SymFilterCompTree>> postProcessed = new HashMap<>();

        // limit the number of trees generated from one single source, sort by their score
        for (Map.Entry<SymbolicFilter, List<SymFilterCompTree>> i : result.entrySet()) {
            List<SymFilterCompTree> trees = i.getValue();
            if (trees.size() > 5) {
                trees.sort(new Comparator<SymFilterCompTree>() {
                    @Override
                    public int compare(SymFilterCompTree o1, SymFilterCompTree o2) {
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

        AbstractSymbolicTable symTable = coreSymTableNFilter.get().getKey();
        SymbolicFilter sf = coreSymTableNFilter.get().getValue();


        Set<SymbolicFilter> sfSet = new HashSet<>();
        sfSet.add(sf);
        List<SymFilterCompTree> trees = symTable.batchGenDecomposition(sfSet).get(sf);
        List<TableNode> cores = new ArrayList<>();
        for (SymFilterCompTree tr : trees) {
            cores.addAll(tr.translateToTopSQL(ec));
        }

        List<TableNode> tableSrcs = new ArrayList<>();
        for(TableNode core : cores) {
            tableSrcs.add(RenameTNWrapper.tryRename(((AggregationNode)((RenameTableNode)baseTableSrc).getTableNode()).substCoreTable(core)));
        }

        return tableSrcs;
    }

    @Override
    public double estimatePrimitiveSymFilterCost(SymbolicFilter sf) {
        return this.decodedPrimitives.get(sf).getKey();
    }

}
