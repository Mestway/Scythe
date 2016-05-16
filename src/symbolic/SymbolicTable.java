package symbolic;

import com.sun.org.apache.regexp.internal.RE;
import enumerator.primitive.EnumCanonicalFilters;
import enumerator.context.EnumContext;
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
import util.CombinationGenerator;
import util.Pair;

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
    private Table baseTable;

    private Set<SymbolicFilter> symbolicPrimitiveFilters = new HashSet<>();
    // after primitveFiltersEvaluated is done, the list will be ready to be used.
    private List<SymbolicFilter> primitives = new ArrayList<>();
    private boolean primitiveFiltersEvaluated = false;

    private SymbolicTable() {}

    public SymbolicTable(Table baseTable, TableNode baseTableSrc) {
        this.baseTable = baseTable;
        this.baseTableSrc = baseTableSrc;
        // the symbolic primitive filters are not evaluated here because we want to keep them lazy
        this.symbolicPrimitiveFilters = new HashSet<>();
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
    public Table getBaseTable() { return this.baseTable; }

    public void setBaseTable(Table baseTable) { this.baseTable = baseTable; }

    public Set<SymbolicFilter> getSymbolicFilters() { return this.symbolicPrimitiveFilters; }

    public void setSymbolicFilters(Set<SymbolicFilter> filters) {
        this.symbolicPrimitiveFilters = filters;
        this.symbolicPrimitiveFilters.add(SymbolicFilter.genSymbolicFilter(this.baseTable, new EmptyFilter()));
    }

    public void emitFinalVisitAllTables(
            MappingInference mi,
            EnumContext ec,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {

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
                f.accept(new Pair<>(this, sf), fl);
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

            if (tn instanceof NamedTable) {
                return EnumCanonicalFilters
                    .enumCanonicalFilterNamedTable((NamedTable) tn, ec)
                    .stream()
                    .filter(f -> {
                        try {
                            return SymbolicFilter.genSymbolicFilter(tn.eval(new Environment()), f).equals(sf);
                        } catch (SQLEvalException e) {
                            return false;
                        }
                    }).collect(Collectors.toList());
            } else {
                return EnumCanonicalFilters.enumCanonicalFilterAggrNode((RenameTableNode) tn, ec)
                        .stream()
                        .filter(f -> {
                            try {
                                return SymbolicFilter.genSymbolicFilter(tn.eval(new Environment()), f).equals(sf);
                            } catch (SQLEvalException e) {
                                return false;
                            }
                        }).collect(Collectors.toList());
            }
        } catch (SQLEvalException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<TableNode> decodeToQuery(Pair<AbstractSymbolicTable, SymbolicFilter> sfp, EnumContext ec, FilterLinks fl) {

        // this decoder can only decode the first on the table
        assert (sfp.getKey().equals(this));
        Set<Set<Pair<AbstractSymbolicTable, SymbolicFilter>>> srcSet = fl.retrieveSource(sfp);

        TableNode tn = this.baseTableSrc;

        List<TableNode> result = new ArrayList<>();

        List<ValNode> vals = tn.getSchema().stream()
                .map(s -> new NamedVal(s))
                .collect(Collectors.toList());

        if (srcSet == null) {
            // the filter itself is already a primitive filter
            // return this.decodePrimitiveFilter(sfp.getValue(), ec);
            for (Filter f : this.decodePrimitiveFilter(sfp.getValue(), tn, ec)) {
                result.add(new SelectNode(vals, tn, f));
            }
        } else {
            for (Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src : srcSet) {
                List<List<Filter>> filterList = new ArrayList<>();
                for (Pair<AbstractSymbolicTable, SymbolicFilter> p : src) {
                    // the src link of these tables should be equal to the baseTable
                    assert p.getKey().equals(this);

                    filterList.add(this.decodePrimitiveFilter(p.getValue(), tn, ec));
                }
                List<List<Filter>> rotated = CombinationGenerator.rotateList(filterList);
                for (List<Filter> filters : rotated) {
                    result.add(new SelectNode(vals, tn, LogicAndFilter.connectByAnd(filters)));
                }
            }
        }

        return result;
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
            symfilters.add(symbolic.SymbolicFilter.genSymbolicFilter(this.baseTable, f));
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
}
