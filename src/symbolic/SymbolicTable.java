package symbolic;

import enumerator.primitive.EnumCanonicalFilters;
import enumerator.primitive.FilterEnumerator;
import enumerator.context.EnumContext;
import enumerator.primitive.OneStepQueryInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import util.CombinationGenerator;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SymbolicTable
 * Created by clwang on 3/26/16.
 */
public class SymbolicTable extends AbstractSymbolicTable {

    // what is the base table derived from, there should be an edge:
    // tableSrc --> baseTable
    boolean isFromAggregation = false;

    private Table baseTable;
    private Set<SymbolicFilter> symbolicPrimitiveFilters = new HashSet<>();
    private boolean primitiveFiltersEvaluated = false;

    public SymbolicTable(Table baseTable) {
        this.baseTable = baseTable;
        // the symbolic primitive filters are not evaluated here because we want to keep them lazy
        this.symbolicPrimitiveFilters = new HashSet<>();
    }

    public void setIsFromAggregation() {
        this.isFromAggregation = true;
    }

    @Override
    public Table getBaseTable() { return this.baseTable; }

    public void setBaseTable(Table baseTable) { this.baseTable = baseTable; }

    public Set<SymbolicFilter> getSymbolicFilters() { return this.symbolicPrimitiveFilters; }

    public void setSymbolicFilters(Set<SymbolicFilter> filters) {
        this.symbolicPrimitiveFilters = filters;
        this.symbolicPrimitiveFilters.add(SymbolicFilter.genSymbolicFilter(this.baseTable, new EmptyFilter()));
    }

    // give the maximum filter length,
    // instantiate all possible tables that can be generated from filtering the current table.
    // Currently the max-length is fixed as 2
    @Override
    public Pair<Set<SymbolicFilter>, FilterLinks> instantiateAllFilters() {
        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;
        return AbstractSymbolicTable.mergeAndLinkFilters(this,
                this.symbolicPrimitiveFilters.stream().collect(Collectors.toList()));
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

        TableNode bt = new NamedTable(this.baseTable);
        List<TableNode> tns = new ArrayList<>();
        if (this.isFromAggregation) {
            for (Table t : ec.getInputs()) {
                tns.addAll(OneStepQueryInference.infer(Arrays.asList(new NamedTable(t)), this.baseTable, ec));
            }
        } else {
            tns.add(bt);
        }

        List<TableNode> result = new ArrayList<>();

        for (TableNode tn : tns) {

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
        List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterNamedTable(new NamedTable(this.baseTable), ec);
        ec.setMaxFilterLength(backUpMaxFilterLength);

        // the empty filter is added here to make it complete.
        filters.add(new EmptyFilter());

        Set<symbolic.SymbolicFilter> symfilters = new HashSet<>();
        for (Filter f : filters) {
            symfilters.add(symbolic.SymbolicFilter.genSymbolicFilter(this.baseTable, f));
        }
        this.symbolicPrimitiveFilters = symfilters;
        this.primitiveFiltersEvaluated = true;
    }

    @Override
    public TableNode queryForBaseTable(EnumContext ec) {
        if (this.isFromAggregation) {
            List<TableNode> inferResult = new ArrayList<>();
            for (Table t : ec.getInputs()) {
                inferResult.addAll(OneStepQueryInference.infer(Arrays.asList(new NamedTable(t)), this.baseTable, ec));
            }
            return inferResult.get(0);
        } else {
            return new NamedTable(this.baseTable);
        }
    }

}
