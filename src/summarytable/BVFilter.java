package summarytable;

import sql.lang.datatype.Value;
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
 * Bit-vector encoded filters for tables (filterRep),
 * Positive bits indicate rows are passed the filter condition.
 *
 */
public class BVFilter {

    // refer to the table, not sure if it is necessary
    int rowNumber = -1;
    Set<Integer> filterRep = new HashSet<>();

    public BVFilter(Set<Integer> filterRep, int rowNumber) {
        this.filterRep = filterRep;
        this.rowNumber = rowNumber;
    }

    public Set<Integer> getFilterRep() {
        return this.filterRep;
    }

    public static BVFilter genSymbolicFilterFromTableNode(TableNode tn, Filter f) {
        try {
            return genSymbolicFilter(tn.eval(new Environment()), f);
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BVFilter genSymbolicFilter(Table table, Filter filter) {

        Set<Integer> filteredRows = new HashSet<>();

        for (int r = 0; r < table.getContent().size(); r ++) {

            TableRow row = table.getContent().get(r);
            // The name and value binding between field names and values
            Map<String, Value> rowBinding = new HashMap<String, Value>();

            // extend the evaluation environment for this column
            for (int i = 0; i < table.getSchema().size(); i ++) {
                String valName = (table.getName().equals("anonymous") ? "" : table.getName() + ".")
                         + table.getSchema().get(i);
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

        return new BVFilter(filteredRows, table.getContent().size());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BVFilter) {
            if (this.rowNumber != ((BVFilter) o).rowNumber) return false;
            if (this.filterRep.size() == ((BVFilter) o).filterRep.size()) {
                for (Integer containedRow : filterRep) {
                    if (! ((BVFilter) o).filterRep.contains(containedRow))
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
    public static BVFilter mergeFilter(
            BVFilter f1,
            BVFilter f2,
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
        return new BVFilter(mergedFilterRep, f1.rowNumber);
    }

    // this is used to determine whether sf2 is fully contained in sf1,
    // i.e. whether all elements in sf2 are contained in sf1
    public boolean fullyContained(BVFilter sf2) {
        assert this.rowNumber == sf2.rowNumber;
        return this.getFilterRep().containsAll(sf2.filterRep);
    }

    // check whether each range is covered by at least one bit
    public boolean rangeFullyContained(List<Set<Integer>> targetRanges) {
        for (Set<Integer> range : targetRanges) {
            boolean contained = false;
            for (Integer i : range) {
                if (filterRep.contains(i)) {
                    contained = true;
                    break;
                }
            }
            if (contained == false)
                return false;
        }
        return true;
    }

    // check whether each range is covered by at least one bit
    public boolean exactMatchRange(List<Set<Integer>> targetRanges) {
        return this.rangeFullyContained(targetRanges) &&
                targetRanges.stream().flatMap(x -> x.stream()).collect(Collectors.toList()).containsAll(this.filterRep);
    }

    @Override
    public String toString() {
        String s = "[";
        for (int i : this.filterRep) {
            s += i + " ";
        }
        return s + ": " + this.rowNumber +"]";
    }

    //Determine whether the filter is built from an empty filter
    public boolean isEmptyFilter() {
        if (this.rowNumber == this.filterRep.size())
            return true;
        return false;
    }

    public static BVFilter genEmptyFilter(int rowNumber) {
        Set<Integer> filterRep = new HashSet<>();
        for (int i = 0; i < rowNumber; i ++) {
            filterRep.add(i);
        }
        return new BVFilter(filterRep, rowNumber);
    }
}
