package sql.lang;

import javafx.util.Pair;
import sql.lang.DataType.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by clwang on 12/14/15.
 */
public class TableRow {
    /* the table containing the row */
    String tableName = "";
    List<String> fieldNames = new ArrayList<String>();
    List<Value> values = new ArrayList<Value>();

    private TableRow() {}

    public static TableRow TableRowFromString(String tableName, List<String> names, List<String> rowContent) {
        TableRow newRow = new TableRow();
        newRow.tableName = tableName;

        for (String s : names) {
            newRow.fieldNames.add(s);
        }
        for (String s : rowContent) {
            newRow.values.add(Value.parse(s));
        }

        return newRow;
    }

    public static TableRow TableRowFromContent(String tableName, List<String> names, List<Value> rowContent) {
        TableRow newRow = new TableRow();
        newRow.tableName = tableName;

        for (String s : names) {
            newRow.fieldNames.add(s);
        }
        for (Value v : rowContent) {
            newRow.values.add(v);
        }
        return newRow;
    }

    // duplicate the current row
    public TableRow duplicate() {
        TableRow tr = new TableRow();
        tr.tableName = this.tableName;
        for (String s : fieldNames) {
            tr.fieldNames.add(s);
        }
        for (Value v : values) {
            tr.values.add(v.duplicate());
        }
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

    public TableRow projection(List<Integer> projectionIndex) {
        TableRow newRow = new TableRow();
        for (Integer i : projectionIndex) {
            newRow.fieldNames.add(this.fieldNames.get(i));
            newRow.values.add(this.values.get(i).duplicate());
        }
        return newRow;
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

    public boolean equals(TableRow row) {
        for (int i = 0; i < this.values.size(); i ++) {
            if (!this.values.get(i).equals(row.values.get(i))) {
                return false;
            }
            if (!this.fieldNames.get(i).equals(row.fieldNames.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean contentEquals(TableRow row) {
        for (int i = 0; i < this.values.size(); i ++) {
            if (!this.values.get(i).equals(row.values.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static Pair<String, String> ColumnNameParser(String quantifiedName) {
        if (quantifiedName.indexOf('.') == -1) {
            return new Pair<String, String>("", quantifiedName);
        }
        String tableName = quantifiedName.substring(0, quantifiedName.indexOf("."));
        String columnName = quantifiedName.substring(quantifiedName.indexOf(".") + 1);
        return new Pair<String, String>(tableName, columnName);
    }

    public void updateTableName(String tableName) {
        this.tableName = tableName;
    }
    public void updateMetadata(List<String> metadata) {
        this.fieldNames.clear();
        for (String s : metadata) {
            this.fieldNames.add(s);
        }
    }

    public List<Value> retrieveValuesByIndices(List<Integer> indices) {
        List<Value> result = new ArrayList<Value>();
        for (Integer i : indices) {
            result.add(this.getValue(i));
        }
        return result;
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
}
