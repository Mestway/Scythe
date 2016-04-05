package symbolic;

import sql.lang.Table;
import util.CombinationGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 */
public abstract class AbstractSymbolicTable {

    // this determines that the method "combining filters" will only generate filters of length 2
    static int maxFilterLength = 2;
    static BiFunction<Boolean, Boolean, Boolean> mergeFunction = (x, y) -> x && y;

    abstract public Table getBaseTable();
    abstract public Set<SymbolicFilter> instantiateAllFilters();
    abstract public int getPrimitiveFilterNum();

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
        return instantiatedTables.stream().filter(t -> t.getContent().size() > 0).collect(Collectors.toList());
    }

    public static Set<SymbolicFilter> combiningFilters(Set<SymbolicFilter> basicFilters) {

        Set<SymbolicFilter> mergedFilters = new HashSet<>();

        mergedFilters.addAll(basicFilters);

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
