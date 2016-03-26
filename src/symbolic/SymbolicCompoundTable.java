package symbolic;

import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.JoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;
import sun.jvm.hotspot.debugger.cdbg.Sym;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Created by clwang on 3/26/16.
 */
public class SymbolicCompoundTable extends AbstractSymbolicTable {

    AbstractSymbolicTable st1, st2;

    // the set of filters that use both st1 elements and st2 elements,
    // original filters on st1 and st2 are not contained here
    Set<SymbolicFilter> filtersLR = new HashSet<>();
    public SymbolicCompoundTable(AbstractSymbolicTable st1,  AbstractSymbolicTable st2, Set<SymbolicFilter> filters) {
        this.st1 = st1;
        this.st2 = st2;
        this.filtersLR = filters;
    }

    @Override
    public Table getBaseTable() {
        return Table.joinTwo(st1.getBaseTable(), st2.getBaseTable());
    }

    @Override
    public Set<SymbolicFilter> instantiateAllFilters() {
        Set<SymbolicFilter> st1Filters = st1.instantiateAllFilters();
        Set<SymbolicFilter> st2Filters = st2.instantiateAllFilters();
        Set<SymbolicFilter> promotedFilters = new HashSet<>();
        for (SymbolicFilter f1 : st1Filters) {
            for (SymbolicFilter f2 : st2Filters) {
                List<Integer> fr = new ArrayList<>();
                for (Integer i : f1.getFilterRep()) {
                    for (Integer j : f2.getFilterRep()) {
                        fr.add(i * f2.rowNumber + j);
                    }
                }
                promotedFilters.add(new SymbolicFilter(fr, f1.rowNumber * f2.rowNumber));
            }
        }
        Set<SymbolicFilter> mergeFilter = AbstractSymbolicTable.combiningFilters(filtersLR);

        Set<SymbolicFilter> resultFilters = new HashSet<>();
        for (SymbolicFilter f1 : promotedFilters) {
            for (SymbolicFilter f2 : mergeFilter) {
                List<Integer> fr = new ArrayList<>();
                for (int i = 0; i < f1.rowNumber; i ++) {
                    if (f1.getFilterRep().contains(i) && f2.getFilterRep().contains(i)) {
                        fr.add(i);
                    }
                }
                resultFilters.add(new SymbolicFilter(fr, f1.rowNumber));
            }
        }

        return resultFilters;
    }
}
