package lang.sql.ast.abstable;


import forward_enumeration.context.EnumContext;
import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.ValNode;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.table.Table;
import lang.table.TableRow;
import util.IndentationManager;
import util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/16/15.
 */
public class AbsSelectNode extends AbsTableNode {

    List<ValNode> columns;
    AbsTableNode tableNode;

    public AbsSelectNode(List<ValNode> columns, AbsTableNode node) {
        super();
        this.columns = columns;
        this.tableNode = node;
    }

    // abstract select does not perform filtering
    @Override
    public Table eval(Environment env) throws SQLEvalException {

        Table table = tableNode.eval(env);

        Table resultTable = table.duplicate();

        // initialize result table data
        resultTable.getSchema().clear();
        for (ValNode vn : columns) {
            resultTable.getSchema().add(vn.getName());
        }

        // initialize result table content
        resultTable.getContent().clear();
        resultTable.updateName("anonymous");

        for (TableRow row : table.getContent()) {
            // The name and value binding between field names and values
            Map<String, Value> rowBinding = new HashMap<String, Value>();

            // extend the evaluation environment for this column
            if (table.getName().equals("anonymous")) {
                for (int i = 0; i < table.getSchema().size(); i ++) {
                    rowBinding.put(table.getSchema().get(i), row.getValue(i));
                }
            } else {
                for (int i = 0; i < table.getSchema().size(); i ++) {
                    rowBinding.put(table.getName() + "." + table.getSchema().get(i), row.getValue(i));
                }
            }

            Environment extEnv = env.extend(rowBinding);

            List<Value> rowContent = new ArrayList<Value>();
            for (ValNode vn : this.columns) {
                rowContent.add(vn.eval(extEnv));
            }
            TableRow newRow = TableRow.TableRowFromContent(
                    resultTable.getSchema(),
                    rowContent);

            resultTable.getContent().add(newRow);
        }

        return resultTable;
    }

    @Override
    public List<String> getSchema() {
        List<String> schema = new ArrayList<String>();
        for (ValNode vn : columns) {
            schema.add(vn.getName());
        }
        return schema;
    }

    @Override
    public String getTableName() {
        return "anonymous";
    }

    @Override
    public List<ValType> getSchemaType() {
        List<String> tableSchema = this.tableNode.getSchema();
        List<ValType> tableSchemaType = this.tableNode.getSchemaType();

        List<ValType> schemaTypes = new ArrayList<ValType>();

        for (ValNode n : columns) {
            if (n instanceof NamedVal) {
                String name = n.getName();
                if (tableNode.getTableName().equals("anonymous"))
                    name = n.getName().substring(n.getName().indexOf(".") + 1);
                for (int i = 0; i < tableSchema.size(); i ++) {
                    if (tableSchema.get(i).equals(name)) {
                        schemaTypes.add(tableSchemaType.get(i));
                        break;
                    }
                }
            } else {
                schemaTypes.add(n.getType(new EnumContext()));
            }
        }

        return schemaTypes;
    }

    @Override
    public int getNestedQueryLevel() {
        return tableNode.getNestedQueryLevel() + 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {

        // determine if it is select all
        boolean selectFieldsAllSame = false;
        if (tableNode.getSchema().size() == columns.size()) {
            selectFieldsAllSame = true;
            for (int i = 0; i < tableNode.getSchema().size(); i ++) {
                if (! tableNode.getSchema().get(i).equals(columns.get(i).prettyPrint(0))) {
                    selectFieldsAllSame = false;
                    break;
                }
            }
        }

        if (selectFieldsAllSame) {
            return IndentationManager.addIndention( tableNode.prettyPrint(0, true), indentLv);
        }

        String result = "";

        result += "Select ";

        String selectArg = "";

        boolean flag = true;
        for (ValNode s : this.columns) {
            if (flag) {
                selectArg += s.prettyPrint(0);
                flag = false;
            } else
                selectArg += "," + s.prettyPrint(0);
        }

        if (selectFieldsAllSame)
            result += "*\r\n";
        else
            result += selectArg + "\r\n";

        result += " From\r\n";

        result += tableNode.prettyPrint(1, true);

        result += "\r\n";
        result += " Where ▢";

        if (asSubquery)
            result = "(" + result + ")";
        return IndentationManager.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        columns.forEach(c -> result.addAll(c.getAllHoles()));
        result.addAll(tableNode.getAllHoles());
        return result;
    }

    @Override
    public AbsTableNode instantiate(InstantiateEnv env) {
        return new AbsSelectNode(
                this.columns.stream()
                        .map(c -> c.instantiate(env)).collect(Collectors.toList()),
                tableNode.instantiate(env));
    }

    @Override
    public AbsTableNode substNamedVal(ValNodeSubstitution vnsb) {
        return new AbsSelectNode(
                this.columns.stream().map(c -> c.subst(vnsb)).collect(Collectors.toList()),
                tableNode.substNamedVal(vnsb));
    }

    @Override
    public List<AbsNamedTable> namedTableInvolved() {
        List<AbsTableNode> result = new ArrayList<>();
        // TODO: add filter stuff in future
        return tableNode.namedTableInvolved();
    }

    @Override
    public AbsTableNode tableSubst(List<Pair<AbsTableNode,AbsTableNode>> pairs) {

        AbsTableNode core = this.tableNode.tableSubst(pairs);

        List<String> currentSchema = tableNode.getSchema();
        List<String> newSchema = core.getSchema();

        ValNodeSubstitution vnsb = new ValNodeSubstitution();
        for (int i = 0; i < currentSchema.size(); i++) {
            vnsb.addBinding(
                    new Pair<>(
                            new NamedVal(currentSchema.get(i)),
                            new NamedVal(newSchema.get(i))));
        }

        AbsTableNode sn = new AbsSelectNode(
            columns.stream().map(c -> c.subst(vnsb)).collect(Collectors.toList()),
            core);

        return sn;
    }

    @Override
    public List<String> originalColumnName() {
        List<Integer> indices = new ArrayList<>();
        for (ValNode c : this.columns) {
            if (! (c instanceof NamedVal))
                indices.add(-1);
            else {
                boolean added = false;
                for (int i = 0; i < this.tableNode.getSchema().size(); i ++) {
                    if (this.tableNode.getSchema().get(i).equals(c.getName())) {
                        indices.add(i);
                        added = true;
                        break;
                    }
                }
                if (! added) {
                    System.err.println("[SelectNode250] Unrecognized schema.");
                    indices.add(-1);
                }
            }
        }
        List<String> innerLevelColumnName = this.tableNode.originalColumnName();
        List<String> result = new ArrayList<>();
        for (Integer i : indices) {
            if (i == -1) {
                result.add("none-col-name");
            } else {
                result.add(innerLevelColumnName.get(i));
            }
        }
        return result;
    }

    public AbsTableNode getTableNode() { return this.tableNode; }
    public List<ValNode> getColumns() { return this.columns; }


    @Override
    public List<Value> getAllConstants() {
        List<Value> list = new ArrayList<>();
        list.addAll(tableNode.getAllConstants());
        return list;
    }

    public String getQuerySkeleton() {
        return "(S " + tableNode.getQuerySkeleton() + ")";
    }

}
