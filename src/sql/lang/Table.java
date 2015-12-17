package sql.lang;

import sql.lang.ast.filter.VVComparator;

import java.util.*;

/**
 * Created by clwang on 12/12/15.
 * The class representing a Table in a database
 */
public class Table {

    static int TableCount = 0;
    public static String AssignNewName() {
        TableCount ++;
        return "DefaultTableName" + TableCount;
    }

    String name = "";
    List<String> metadata = new ArrayList<String>();
    List<TableRow> rows = new ArrayList<TableRow>();

    public Table() {}

    /**
     * Initializer of a table, the content here is only strings
     * @param tableName the name of the table
     * @param metadata the meta data of the table
     * @param rawContent all contents in the form of string
     */
    public Table(String tableName, List<String> metadata, List<List<String>> rawContent) {
        List<TableRow> rows = new ArrayList<TableRow>();
        for (List<String> sList : rawContent) {
            rows.add(new TableRow(tableName, metadata, sList));
        }
        this.initialize(tableName, metadata, rows);
    }

    public void initialize(String tableName, List<String> metadata, List<TableRow> rows) {
        this.name = tableName;
        this.metadata = metadata;
        this.rows = rows;
    }

    public List<String> getMetadata() { return this.metadata; }
    public String getName() { return this.name; }
    public List<TableRow> getContent() { return this.rows; }
    public void setName(String tableName) { this.name = tableName; }

    @Override
    public String toString() {
        String str = "@" + name + "\r\n";
        for (String i : metadata) {
            str += i + " | ";
        }
        str = str.substring(0, str.length() - 2) + "\r\n";
        for (TableRow row : rows) {
            str += row.toString() + "\r\n";
        }
        return str;
    }

    // duplicate the current table
    // (All values in the table duplicated)
    public Table duplicate() {
        Table table = new Table();
        table.name = this.name;
        for (String i : this.metadata) {
            table.metadata.add(i);
        }
        for (TableRow valList : this.rows) {
            table.rows.add(valList.duplicate());
        }
        return table;
    }

    public Table projectionBy(List<String> columns) {
        List<Integer> columnIndices = new ArrayList<Integer>();
        for (String s : columns) {
            for (int i = 0; i < this.metadata.size(); i ++) {
                if (this.metadata.get(i).equals(s)) {
                    columnIndices.add(i);
                }
            }
        }

        Table newTable = new Table();
        newTable.name = this.name;
        for (Integer i : columnIndices) {
            newTable.metadata.add(this.metadata.get(i));
        }
        for (TableRow row : this.rows) {
            newTable.rows.add(row.projection(columnIndices));
        }
        return newTable;
    }

    public boolean tableEquals(Table table) {
        for (int i = 0; i < this.metadata.size(); i ++) {
            if (!this.metadata.get(i).equals(table.metadata.get(i)))
                return false;
        }
        for (int i = 0; i < this.rows.size(); i ++) {
            if (! this.rows.get(i).equals(table.rows.get(i))) {
                return false;
            }
        }
        return true;
    }
}
