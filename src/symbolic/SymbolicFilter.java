package symbolic;

import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;

import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 * This represent a primitive filter of a table,
 * indicating that the filter is positive to the rows stored in filterRep
 */
public class SymbolicFilter {

    // refer to the table, not sure if it is necessary
    int rowNumber = -1;
    Set<Integer> filterRep = new HashSet<>();

    public SymbolicFilter(Set<Integer> filterRep, int rowNumber) {
        this.filterRep = filterRep;
        this.rowNumber = rowNumber;
    }

    public Set<Integer> getFilterRep() {
        return this.filterRep;
    }

    public static SymbolicFilter genSymbolicFilterFromTableNode(TableNode tn, Filter f) {
        try {
            return genSymbolicFilter(tn.eval(new Environment()), f);
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SymbolicFilter genSymbolicFilter(Table table, Filter filter) {

        Set<Integer> filteredRows = new HashSet<>();

        for (int r = 0; r < table.getContent().size(); r ++) {

            TableRow row = table.getContent().get(r);
            // The name and value binding between field names and values
            Map<String, Value> rowBinding = new HashMap<String, Value>();

            // extend the evaluation environment for this column
            for (int i = 0; i < table.getMetadata().size(); i ++) {
                String valName = (table.getName().equals("anonymous") ? "" : table.getName() + ".")
                         + table.getMetadata().get(i);
                Value val = row.getValue(i);
                //System.out.println(valName + " : " + val.toString());
                rowBinding.put(valName, val);
            }

            Environment extEnv = new Environment().extend(rowBinding);

            try {
                if (filter.filter(extEnv)) {
                    filteredRows.add(r);
                }
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }

        return new SymbolicFilter(filteredRows, table.getContent().size());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SymbolicFilter) {
            if (this.rowNumber != ((SymbolicFilter) o).rowNumber) return false;
            if (this.filterRep.size() == ((SymbolicFilter) o).filterRep.size()) {
                for (Integer containedRow : filterRep) {
                    if (! ((SymbolicFilter) o).filterRep.contains(containedRow))
                        return false;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        Integer hash = 0, prime = 31;
        List<Integer> fr = filterRep.stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < fr.size(); i++) {
            hash = hash + prime * hash + fr.get(i);
        }
        return hash;
    }

    // This should be performed on
    public static SymbolicFilter mergeFilter(
            SymbolicFilter f1,
            SymbolicFilter f2,
            BiFunction<Boolean, Boolean, Boolean> mergeFunction) {
        if (! (f1.rowNumber == f2.rowNumber))
            System.err.println("[SymbolicPrimitiveFilter 99] Merging two incompatible filters.");
        Set<Integer> mergedFilterRep = new HashSet<>();
        for (int i = 0; i < f1.rowNumber; i ++) {
            boolean valid1 = false, valid2 = false;
            if (f1.filterRep.contains(i)) valid1 = true;
            if (f2.filterRep.contains(i)) valid2 = true;
            if (mergeFunction.apply(valid1, valid2)) {
                mergedFilterRep.add(i);
            }
        }
        return new SymbolicFilter(mergedFilterRep, f1.rowNumber);
    }

    @Override
    public String toString() {
        String s = "";
        for (int i : this.filterRep) {
            s += i + " ";
        }
        return s + " : " + this.rowNumber;
    }
}
