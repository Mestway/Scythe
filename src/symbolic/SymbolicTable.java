package symbolic;

import enumerator.primitive.EnumCanonicalFilters;
import enumerator.primitive.FilterEnumerator;
import enumerator.context.EnumContext;
import sql.lang.Table;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.NamedTable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SymbolicTable
 * Created by clwang on 3/26/16.
 */
public class SymbolicTable extends AbstractSymbolicTable {

    private Table baseTable;
    private Set<SymbolicFilter> symbolicPrimitiveFilters = new HashSet<>();

    public SymbolicTable(Table baseTable, Set<SymbolicFilter> filters) {
        this.baseTable = baseTable;
        this.symbolicPrimitiveFilters = filters;
        filters.add(SymbolicFilter.genSymbolicFilter(this.baseTable, new EmptyFilter()));
    }

    @Override
    public Table getBaseTable() { return this.baseTable; }

    public void setBaseTable(Table baseTable) { this.baseTable = baseTable; }

    public Set<SymbolicFilter> getSymbolicFilters() { return this.symbolicPrimitiveFilters; }
    public void setSymbolicFilters(Set<SymbolicFilter> filters) {
        this.symbolicPrimitiveFilters = filters;
        filters.add(SymbolicFilter.genSymbolicFilter(this.baseTable, new EmptyFilter()));
    }

    // give the maximum filter length,
    // instantiate all possible tables that can be generated from filtering the current table.
    // Currently the max-length is fixed as 2
    @Override
    public Set<SymbolicFilter> instantiateAllFilters() {
        Set<SymbolicFilter> symf = combiningFilters(this.symbolicPrimitiveFilters);
        return symf;
    }

    @Override
    public int getPrimitiveFilterNum() {
        return this.symbolicPrimitiveFilters.size();
    }

    public static SymbolicTable buildSymbolicTable(Table t, EnumContext ec) {
        List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterNamedTable(new NamedTable(t), ec);
        // the empty filter is added here to make it complete.
        filters.add(new EmptyFilter());

        Set<SymbolicFilter> symfilters = new HashSet<>();
        for (Filter f : filters) {
            symfilters.add(SymbolicFilter.genSymbolicFilter(t, f));
        }
        return new SymbolicTable(t, symfilters);
    }

}
