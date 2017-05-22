package sql.lang;

import sql.lang.val.NullVal;
import sql.lang.val.ValType;
import util.Pair;
import sql.lang.val.Value;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/14/15.
 */
public class TableRow {
    /* the table containing the row */
    List<String> fieldNames = new ArrayList<String>();
    List<Value> values = new ArrayList<Value>();

    private TableRow() {}

    public static TableRow TableRowFromString(String tableName, List<String> names, List<String> rowContent) {
        TableRow newRow = new TableRow();

        for (String s : names)
            newRow.fieldNames.add(s);
        for (String s : rowContent)
            newRow.values.add(Value.parse(s));

        return newRow;
    }

    public static TableRow TableRowFromContent(String tableName, List<String> names, List<Value> rowContent) {
        TableRow newRow = new TableRow();
        newRow.fieldNames.addAll(names);
        newRow.values.addAll(rowContent);
        return newRow;
    }

    // duplicate the current row
    public TableRow duplicate() {
        TableRow tr = new TableRow();
        tr.fieldNames.addAll(fieldNames);
        tr.values.addAll(values.stream().map(Value::duplicate).collect(Collectors.toList()));
        return tr;
    }

    public List<Value> getValues() {
        return this.values;
    }

    public Value getValue(int i) {
        if (this.values.size() <= i) {
            System.err.println("[Error@TableRow59] None Exist Value");
        }
        return this.values.get(i);
    }

    @Override
    public String toString() {
        String str = "";
        for (Value i : values) {
            str += i.toString() + " | ";
        }
        // ignore the last "|"
        str = str.substring(0, str.length() - 2);
        return str;
    }

    @Override
    public int hashCode() {
        return this.values.stream().map(value -> value.hashCode()).reduce(0, (x,y) -> (x + y) % 179426549);
    }

    @Override
    public boolean equals(Object obj) {

        if (! (obj instanceof TableRow))
            return false;
        TableRow row = (TableRow) obj;
        for (int i = 0; i < this.values.size(); i ++) {
            if (!this.values.get(i).equals((row).values.get(i))) {
                return false;
            }
            if (!this.fieldNames.get(i).equals((row).fieldNames.get(i))) {
                return false;
            }
        }
        return true;
    }

    // if the content of the row equals to another row
    public boolean contentEquals(TableRow row) {

        for (int i = 0; i < this.values.size(); i ++) {
            if (!this.values.get(i).equals(row.values.get(i))) {
                return false;
            }
        }
        return true;
    }

    // it returns true if the current row equals r2,
    // or, if the current row is a permutation of another row
    public boolean contentRoughlyEquals(TableRow row) {
        return isPermutation(row.values, this.values);
    }

    public static boolean isPermutation(List<Value> l1, List<Value> l2) {
        List<Pair<Value, Integer>> counter = new ArrayList<>();
        for (Value v : l1) {
            boolean exists = false;
            for (Pair<Value, Integer> p : counter) {
                if (p.getKey().equals(v)) {
                    exists = true;
                    break;
                }
            }
            if (exists == true)
                continue;

            int count = 0;
            for (Value v2 : l1) {
                if (v.equals(v2)) count ++;
            }
            counter.add(new Pair(v, count));
        }

        for (Value v : l2) {
            int count = 0;
            for (Value v2 : l2) {
                if (v.equals(v2)) count ++;
            }

            boolean exists = false;
            for (Pair<Value, Integer> p : counter) {
                if (p.getKey().equals(v)) {
                    exists = true;
                    if (! (p.getValue() == count)) {
                        return false;
                    }
                }
            }
            if (exists == false)
                return false;
        }
        return true;
    }

    public static Pair<String, String> ColumnNameParser(String quantifiedName) {
        if (quantifiedName.indexOf('.') == -1) {
            return new Pair<>("", quantifiedName);
        }
        String tableName = quantifiedName.substring(0, quantifiedName.indexOf("."));
        String columnName = quantifiedName.substring(quantifiedName.indexOf(".") + 1);
        return new Pair<>(tableName, columnName);
    }

    public void updateMetadata(List<String> metadata) {
        this.fieldNames.clear();
        for (String s : metadata) {
            this.fieldNames.add(s);
        }
    }

    public TableRow projection(List<Integer> projectionIndexes) {
        TableRow r = this.duplicate();
        r.fieldNames = projectionIndexes.stream().map(c -> this.fieldNames.get(c)).collect(Collectors.toList());
        r.values = projectionIndexes.stream().map(c -> this.values.get(c)).collect(Collectors.toList());
        return r;
    }

    public List<Value> retrieveValuesByIndices(List<Integer> indices) {
        List<Value> result = new ArrayList<>();
        for (Integer i : indices) {
            result.add(this.getValue(i));
        }
        return result;
    }

    public TableRow extendWithNull(List<Pair<String, ValType>> extensionSchema) {
        TableRow r = this.duplicate();
        r.fieldNames.addAll(extensionSchema.stream().map(p -> p.getKey()).collect(Collectors.toList()));
        r.values.addAll(extensionSchema.stream().map(p -> new NullVal(p.getValue())).collect(Collectors.toList()));
        return r;
    }

    /**
     * If the current row contains only one value, we will return the value,
     * if not, an exception will be raised
     * @return the only value the row contains
     */
    public Value rowToValue() throws Exception {
        if (this.values.size() != 1) {
            throw new Exception();
        }
        return this.values.get(0);
    }

    public int index = -1;

}
