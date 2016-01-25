package sql.lang.ast.table;

import enumerator.parameterized.InstantiateEnv;
import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import util.IndentionManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/18/15.
 * Join is implemented as cartesian product
 */
public class JoinNode implements TableNode {

    List<TableNode> tableNodes = new ArrayList<TableNode>();

    public JoinNode(List<TableNode> tableNodes) {
        this.tableNodes = tableNodes;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        List<Table> tables = new ArrayList<Table>();
        for (TableNode tn : tableNodes) {
            tables.add(tn.eval(env));
        }
        Table currentTable = tables.get(0);
        for (int i = 1; i < tables.size(); i ++) {
            currentTable = joinTwo(currentTable, tables.get(i));
        }
        return currentTable;
    }

    @Override
    public List<String> getSchema() {
        List<String> schema = new ArrayList<String>();
        for (TableNode tn : this.tableNodes) {
            schema.addAll(tn.getSchema());
        }
        return schema;
    }

    @Override
    public String getTableName() {
        return "anonymous";
    }

    @Override
    public List<ValType> getSchemaType() {
        List<ValType> valTypes = new ArrayList<ValType>();
        for (TableNode tn : this.tableNodes) {
            valTypes.addAll(tn.getSchemaType());
        }
        return valTypes;
    }

    @Override
    // the level of join is the maximum table level + 1
    public int getNestedQueryLevel() {
        int lv = 0;
        for (TableNode tn : this.tableNodes) {
            if (tn.getNestedQueryLevel() > lv)
                lv = tn.getNestedQueryLevel();
        }
        return lv + 1;
    }

    @Override
    public String prettyPrint(int indentLv) {
        String result = "( " + this.tableNodes.get(0).prettyPrint(1).trim() + " )";
        for (int i = 1; i < this.tableNodes.size(); i ++) {
            result += " JOIN (\r\n" + this.tableNodes.get(i).prettyPrint(1) + " )";
        }
        return IndentionManagement.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        this.tableNodes.forEach(tn -> result.addAll(tn.getAllHoles()));
        return result;
    }

    @Override
    public TableNode instantiate(InstantiateEnv env) {
        return new JoinNode(
            this.tableNodes.stream()
                .map(t -> t.instantiate(env)).collect(Collectors.toList()));
    }

    private Table joinTwo(Table t1, Table t2) {
        Table resultTable = new Table();
        List<String> resultTableMetadata = new ArrayList<String>();

        String resultTableName = "anonymous";

        // collect metadata
        for (String md : t1.getMetadata()) {
            if (t1.getName().equals("anonymous")) {
                resultTableMetadata.add(md);
            } else {
                resultTableMetadata.add(t1.getName() + "." + md);
            }
        }
        for (String md : t2.getMetadata()) {
            if (t2.getName().equals("anonymous")) {
                resultTableMetadata.add(md);
            } else {
                resultTableMetadata.add(t2.getName() + "." + md);
            }
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

}
