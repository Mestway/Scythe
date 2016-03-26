package symbolic;

import sql.lang.Table;
import util.CombinationGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Created by clwang on 3/26/16.
 */
public abstract class AbstractSymbolicTable {

    static int maxFilterLength = 2;
    static BiFunction<Boolean, Boolean, Boolean> mergeFunction = (x, y) -> x && y;

    abstract public Table getBaseTable();
    abstract public Set<SymbolicFilter> instantiateAllFilters();

    public List<Table> instantiateAllTables() {
        Set<SymbolicFilter> filters = this.instantiateAllFilters();
        Table table = this.getBaseTable();

        List<Table> instantiatedTables = new ArrayList<>();
        for (SymbolicFilter spf : filters) {
            Table t = table.duplicate();
            t.getContent().clear();
            for (Integer i : spf.getFilterRep()) {
                t.getContent().add(table.getContent().get(i).duplicate());
            }
            instantiatedTables.add(t);
        }
        return instantiatedTables;
    }

    public static Set<SymbolicFilter> combiningFilters(Set<SymbolicFilter> basicFilters) {

        Set<SymbolicFilter> mergedFilters = new HashSet<>();

        // all combinations are generate, from length 0 to length maxFilterLength
        List<List<SymbolicFilter>> filterLists =
                CombinationGenerator.genCombination(new ArrayList<>(basicFilters), maxFilterLength);

        for (List<SymbolicFilter> sfList : filterLists) {
            SymbolicFilter current = sfList.get(0);
            for (int i = 1; i < sfList.size(); i ++) {
                current = SymbolicFilter.mergeFilter(current, sfList.get(i), mergeFunction);
            }
            mergedFilters.add(current);
        }
        return mergedFilters;
    }
}
