package sql.lang;

import backward_inference.MappingInference;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/12/15.
 * The class representing a Table in a database
 */
public class Table {

    private static int TableCount = 0;
    public static String AssignNewName() {
        TableCount ++;
        return "{Default" + TableCount + "}";
    }

    private String name = "";
    private List<String> schema = new ArrayList<>();
    private List<TableRow> rows = new ArrayList<>();

    public Table() {}

    /**
     * Initializer of a table, the content here is only strings
     * @param tableName the name of the table
     * @param schema the meta data of the table
     * @param rawContent all contents in the form of string
     */
    public Table(String tableName, List<String> schema, List<List<String>> rawContent) {
        List<TableRow> rows = rawContent
                .stream()
                .map(sList -> TableRow.TableRowFromString(tableName, schema, sList))
                .collect(Collectors.toList());
        this.initialize(tableName, schema, rows);
    }

    public void initialize(String tableName, List<String> schema, List<TableRow> rows) {
        this.name = tableName;
        this.schema = schema;
        this.rows = rows;
    }

    public List<String> getSchema() { return this.schema; }
    public String getName() { return this.name; }
    public List<TableRow> getContent() { return this.rows; }

    // Update the name of a table
    // Not sure if it is supposed to be used
    public void updateName(String tableName) {
        this.name = tableName;
    }

    public void updateSchema(List<String> schema) {
        if (this.schema.size() != schema.size()) {
            System.err.println("[Error@Table61] the new metadata size does not equal to the original one.");
        }
        this.schema = schema;
        for (TableRow tr : this.rows) {
            tr.updateMetadata(schema);
        }
    }

    @Override
    public String toString() {
        String str = "@" + name + "\r\n";
        int k = 0;
        for (String i : schema) {
            str += i + "(" + this.getSchemaType().get(k) + ")" + " | ";
            k ++;
        }
        str = str.substring(0, str.length() - 2) + "\r\n";
        for (TableRow row : rows) {
            str += row.toString() + "\r\n";
        }
        return str;
    }

    public String toStringWithIndent(String indent) {
        String str = indent + "@" + name + "\r\n";

        str += indent;
        for (String i : schema) {
            str += i + " | ";
        }
        str = str.substring(0, str.length() - 2) + "\r\n";

        str += indent;
        for (TableRow row : rows) {
            str += row.toString() + "\r\n";
            str += indent;
        }
        return str;
    }

    /**
     * Get a column from the table, specified by index
     * @param index the column to be retrieved
     * @return The column represented as a list
     */
    public List<Value> getColumnByIndex(Integer index) {
        if (index >= this.getSchema().size())
            System.err.println("[ERROR@Table107] column index is bigger than the schema size.");

        return this.getContent().stream().map(r -> r.getValue(index)).collect(Collectors.toList());
    }

    public Table projection(List<Integer> projectionIndexes) {
        Table t = this.duplicate();
        t.schema = projectionIndexes.stream().map(c -> t.schema.get(c)).collect(Collectors.toList());
        t.rows.clear();
        t.rows.addAll(this.rows.stream().map(r -> r.projection(projectionIndexes)).collect(Collectors.toList()));
        return t;
    }

    // duplicate the current table
    // (All values in the table duplicated)
    public Table duplicate() {
        Table table = new Table();
        table.name = this.name;
        for (String i : this.schema) {
            table.schema.add(i);
        }
        for (TableRow valList : this.rows) {
            table.rows.add(valList.duplicate());
        }
        return table;
    }

    public boolean tableEquals(Table table) {
        for (int i = 0; i < this.schema.size(); i ++) {
            if (!this.schema.get(i).equals(table.schema.get(i)))
                return false;
        }
        for (int i = 0; i < this.rows.size(); i ++) {
            if (! this.rows.get(i).equals(table.rows.get(i))) {
                return false;
            }
        }
        return true;
    }

    // may have different size
    public boolean contentEquals(Table table) {
        return this.containsContent(table) && table.containsContent(this);
    }

    public boolean contentStrictEquals(Table table) {
        return this.contentEquals(table) && this.getContent().size() == table.getContent().size();
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

        for (int i = 0; i < this.schema.size(); i ++) {
            if (this.schema.get(i).equals(fieldName))
                return i;
        }
        System.err.println("[Error@Table152]Metadata retrieval fail.");
        return -1;
    }

    public List<String> getQualifiedMetadata() {
        if (this.name.equals("anonymous"))
            return this.schema;
        else {
            // add the qualifier
            return this.schema.stream().map(s -> this.name + "." + s).collect(Collectors.toList());
        }
    }


    public List<ValType> storedSchemaType = null;
    // return the schema type of a table.
    public List<ValType> getSchemaType() {

        if (this.storedSchemaType != null) return this.storedSchemaType;

        List<List<ValType>> typeCollections = new ArrayList<>();
        for (int i = 0; i < this.getSchema().size(); i ++) {
            typeCollections.add(new ArrayList<>());
        }
        for (int i = 0; i < this.getContent().size(); i ++) {
            int j = 0;
            for (Value v : this.getContent().get(i).getValues()) {
                typeCollections.get(j).add(v.getValType());
                j ++;
            }
        }
        return typeCollections.stream().map(l -> typeLowerBound(l)).collect(Collectors.toList());
    }
    public ValType typeLowerBound(List<ValType> types) {
        if (types.isEmpty()) return null;
        ValType currentType = types.get(0);
        for (ValType type : types) {
            if (currentType == type) {
                continue;
            }
            return ValType.StringVal;
        }
        return currentType;
    }

    public boolean isEmpty() {
        if (this.getContent().size() == 0)
            return true;
        return false;
    }

    public boolean roughlyEquals(Table t2) {
        if (this.roughlyContainsContent(t2) && t2.roughlyContainsContent(this))
            return true;
        return false;
    }

    // equals is used as strictContentEqual
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Table) {
            return this.contentStrictEquals((Table) obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // TODO: A severe problem
        int hash = 0;
        int prime = 31;
        for (TableRow tr : this.getContent()) {
            for (Value v : tr.getValues()) {
                hash += v.getVal().hashCode() * prime;
            }
        }
        return hash;
        // another hash function
        /*return this.getContent().parallelStream()
                .map(tr -> tr.getValues().parallelStream().map(t -> t.getVal().hashCode())
                        .reduce(0, (a, b) -> (a + b) % prime))
                .reduce(0, (x, y) -> (x + y) % prime );*/
    }

    public boolean roughlyContainsContent(Table t2) {
        for (TableRow tr : this.rows) {
            boolean exists = false;
            for (TableRow tr2 : t2.rows) {
                if (tr.contentRoughlyEquals(tr2))
                    exists = true;
            }
            if (exists == false)
                return false;
        }
        return true;
    }

    public Integer getSizeValue() {
        int r = this.getContent().size();
        int c = this.getContent().get(0).getValues().size();
        return r * r - c * c;
    }

    public static Table joinTwo(Table t1, Table t2) {
        Table resultTable = new Table();
        List<String> resultTableMetadata = new ArrayList<String>();

        String resultTableName = "anonymous";

        Set<String> alreadyUsedName = new HashSet<>();

        // collect metadata
        for (String md : t1.getSchema()) {
            String name;
            if (t1.getName().equals("anonymous")) {
                name = md;
            } else {
                name = t1.getName() + "." + md;
            }
            String originalName = name;
            int i = 0;
            while (alreadyUsedName.contains(name)) {
                name = originalName + i;
                i ++;
            }
            alreadyUsedName.add(name);
            resultTableMetadata.add(name);
        }

        for (String md : t2.getSchema()) {
            String name;
            if (t2.getName().equals("anonymous")) {
                name = md;
            } else {
                name = t2.getName() + "." + md;
            }
            String originalName = name;
            int i = 0;
            while (alreadyUsedName.contains(name)) {
                name = originalName + i;
                i ++;
            }
            alreadyUsedName.add(name);
            resultTableMetadata.add(name);
        }

        // collect content
        List<TableRow> resultTableContent = new ArrayList<TableRow>();
        for (TableRow tr1 : t1.getContent()) {
            for (TableRow tr2 : t2.getContent()) {
                List<Value> body = new ArrayList<Value>();
                for (Value v1 : tr1.getValues()) {
                    body.add(v1.duplicate());
                }
                for (Value v2 : tr2.getValues()) {
                    body.add(v2.duplicate());
                }
                TableRow newTr = TableRow.TableRowFromContent(
                        resultTableName,
                        resultTableMetadata,
                        body);
                resultTableContent.add(newTr);
            }
        }

        resultTable.initialize(
                resultTableName,
                resultTableMetadata,
                resultTableContent);

        return resultTable;
    }

    public static Table extendWithNull(Table t, List<Pair<String, ValType>> extensionSchema) {

        Table result = t.duplicate();

        result.schema = new ArrayList<>();
        result.schema.addAll(t.schema);
        result.schema.addAll(extensionSchema.stream().map(p -> p.getKey()).collect(Collectors.toList()));
        result.rows = t.getContent().stream().map(r -> r.extendWithNull(extensionSchema)).collect(Collectors.toList());

        return result;
    }

    /**
     * Calculate t1 - t2 (returning rows that are in t1 but not in t2)
     */
    public static Table except(Table t1, Table t2) throws SQLEvalException {

        Table t = t1.duplicate();
        t.getContent().clear();

        for (TableRow r1 : t1.getContent()) {
            boolean contained = false;
            for (TableRow r2 : t2.getContent()) {
                if (r2.contentEquals(r1))
                    contained = true;
            }
            if (contained == false) {
                t.getContent().add(r1.duplicate());
            }
        }
        return t;
    }

    public static Table union(Table t1, Table t2) throws SQLEvalException {

        if (t1.getSchema().size() != t2.getSchema().size()) {
            throw new SQLEvalException("Table Schema not equal in join.");
        }

        if (t2.getContent().isEmpty()) return t1;
        if (t1.getContent().isEmpty()) return t2;

        boolean schemaTypeEquiv = true;
        for (int i = 0; i < t1.getSchemaType().size(); i ++) {
            if (! t1.getSchemaType().get(i).equals(t2.getSchemaType().get(i)))
                schemaTypeEquiv = false;
        }

        if (schemaTypeEquiv == false) {
            throw new SQLEvalException("Table Schema not equal in join.");
        }

        Table result = t1.duplicate();
        for (TableRow r : t2.getContent()) {
            result.getContent().add(r);
        }

        result.updateSchema(t1.getSchema());

        return result;
    }

    // Infer projection columns from src to table
    public static List<Integer> inferProjection(Table src, Table tgt) {
        MappingInference mi = MappingInference.buildMapping(src, tgt);
        List<Integer> columns = mi.genColumnMappingInstances().stream()
                .map(s -> s.stream().findFirst().get()).collect(Collectors.toList());
        return columns;
    }

}
