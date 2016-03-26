package symbolic;

import sql.lang.Table;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import util.CombinationGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

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
    }

    @Override
    public Table getBaseTable() { return this.baseTable; }

    public void setBaseTable(Table baseTable) { this.baseTable = baseTable; }

    public Set<SymbolicFilter> getSymbolicFilters() { return this.symbolicPrimitiveFilters; }
    public void setSymbolicFilters(Set<SymbolicFilter> filters) { this.symbolicPrimitiveFilters = filters; }

    // give the maximum filter length,
    // instantiate all possible tables that can be generated from filtering the current table.
    @Override
    public Set<SymbolicFilter> instantiateAllFilters() {
        return combiningFilters(this.symbolicPrimitiveFilters);
    }

}
