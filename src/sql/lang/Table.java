package sql.lang;

import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.ast.filter.VVComparator;
import sql.lang.exception.SQLEvalException;

import java.util.*;
import java.util.stream.Collectors;

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
            rows.add(TableRow.TableRowFromString(tableName, metadata, sList));
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

    // Update the name of a table
    // Not sure if it is supposed to be used
    public void updateName(String tableName) {
        this.name = tableName;
        for (TableRow r : rows) {
            r.updateTableName(tableName);
        }
    }

    public void updateMetadata(List<String> md) {
        if (this.metadata.size() != md.size()) {
            System.err.println("[Error@Table61] the new metadata size does not equal to the original one.");
        }
        this.metadata = md;
        for (TableRow tr : this.rows) {
            tr.updateMetadata(md);
        }
    }

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

    public boolean contentEquals(Table table) {
        return this.containsContent(table) && table.containsContent(this);
    }

    /**
     * Test whether t2 is contained by this table
     * @param t2
     * @return true - t2 is contained by this table
     *          false - t2 is not contained by this table
     */
    public boolean containsContent(Table t2) {
        for (int i = 0; i < t2.rows.size(); i ++) {
            boolean contains = false;
            for (int j = 0; j < this.rows.size(); j ++) {
                if (this.rows.get(j).getValues().size() != t2.rows.get(i).getValues().size())
                    return false;
                if (this.rows.get(j).contentEquals(t2.rows.get(i)))
                    contains = true;
            }
            if (contains == false)
                return false;
        }
        return true;
    }


    public Value tableToValue() throws SQLEvalException {
        if (this.getContent().size() != 1)
            throw new SQLEvalException("table not a val");

        try {
            return this.getContent().get(0).rowToValue();
        } catch (Exception e) {
            new SQLEvalException("table not a val");
        }

        return null;
    }

    public int retrieveIndex(String name) {
        String fieldName = name;
        if (this.name.equals("anonymous"))
            fieldName = name;
        else
            fieldName = name.substring(name.indexOf(".") + 1);

        for (int i = 0; i < this.metadata.size(); i ++) {
            if (this.metadata.get(i).equals(fieldName))
                return i;
        }
        System.err.println("[Error@Table152]Metadata retrieval fail.");
        return -1;
    }

    public List<String> getQualifiedMetadata() {
        if (this.name.equals("anonymous"))
            return this.metadata;
        else {
            // add the qualifier
            return this.metadata.stream().map(s -> this.name + "." + s).collect(Collectors.toList());
        }
    }

    // return the schema type of a table.
    public List<ValType> getSchemaType() {
        List<ValType> lv = new ArrayList<ValType>();
        for (Value v : this.getContent().get(0).getValues()) {
            lv.add(v.getValType());
        }
        return lv;
    }

    public boolean isEmpty() {
        if (this.getContent().size() == 0)
            return true;
        return false;
    }

}
