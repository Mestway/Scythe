package sql.lang;

import javafx.util.Pair;
import sql.lang.DataType.IntVal;
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
    List<String> names = new ArrayList<String>();
    List<Value> values = new ArrayList<Value>();

    private TableRow() {}

    public TableRow(String tableName, List<String> names, List<String> rowContent) {
        this.tableName = tableName;
        for (String s : names) {
            this.names.add(s);
        }
        for (String s : rowContent) {
            this.values.add(Value.parse(s));
        }
    }

    // duplicate the current row
    public TableRow duplicate() {
        TableRow tr = new TableRow();
        tr.tableName = this.tableName;
        for (String s : names) {
            tr.names.add(s);
        }
        for (Value v : values) {
            tr.values.add(v.duplicate());
        }
        return tr;
    }

    /*
    public Optional<Value> getValue(String quantified) {
        Pair<String, String> namePair = ColumnNameParser(quantified);

        if (!namePair.equals("") && !namePair.equals(this.tableName))
            return Optional.empty();

        String columnName = namePair.getValue();
        int index = -1;
        for (int i = 0; i < this.names.size(); i ++) {
            if (this.names.get(i).equals(columnName)) {
                if (index == -1) {
                    index = i;
                } else {
                    System.err.println("[TableRow@38] Two columns with same name: " + this.names.get(i));
                }
            }
        }
        if (index == -1) {
            return Optional.empty();
        }
        return Optional.of(this.values.get(index));
    }*/

    public Value getValue(int i) {
        if (this.values.size() <= i) {
            System.err.println("[Error@TableRow59] None Exist Value");
        }
        return this.values.get(i);
    }

    public TableRow projection(List<Integer> projectionIndex) {
        TableRow newRow = new TableRow();
        for (Integer i : projectionIndex) {
            newRow.names.add(this.names.get(i));
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
            if (!this.names.get(i).equals(row.names.get(i))) {
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

}
