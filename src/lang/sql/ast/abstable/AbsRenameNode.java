package lang.sql.ast.abstable;

import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;

import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.table.Table;
import util.IndentationManager;
import util.Pair;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/21/15.
 */
public class AbsRenameNode extends AbsTableNode {

    String newTableName;
    List<String> newFieldNames;
    AbsTableNode tableNode;

    boolean renameTable = false;
    boolean renameFields = false;

    private AbsRenameNode(String ntn, List<String> nfn, AbsTableNode tn, boolean rt, boolean rf) {
        super();
        this.newFieldNames = nfn;
        this.newTableName = ntn;
        this.renameFields = rf;
        this.tableNode = tn;
        this.renameTable = rt;
    }

    public AbsRenameNode(List<String> nfn, AbsTableNode tn) {
        super();
        this.newFieldNames = nfn;
        this.tableNode = tn;
        this.renameFields = true;
        this.newTableName = tn.getTableName();
    }

    public AbsRenameNode(String tableName, List<String> fieldsName, AbsTableNode tn) {
        super();
        this.newTableName = tableName;
        this.newFieldNames = fieldsName;
        this.tableNode = tn;
        renameTable = true;
        renameFields = true;
    }

    public AbsRenameNode(String newTableName, AbsTableNode tn) {
        super();
        this.newTableName = newTableName;
        this.newFieldNames = tn.getSchema();
        this.tableNode = tn;
        renameTable = true;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        Table table = tableNode.eval(env);
        if (this.renameTable == true) {
            table.updateName(this.newTableName);
        }
        if (this.renameFields == true) {
            table.updateSchema(this.newFieldNames);
        }
        return table;
    }

    @Override
    public String getTableName() {
        return this.newTableName;
    }

    @Override
    public List<ValType> getSchemaType() {
        return tableNode.getSchemaType();
    }

    @Override
    public int getNestedQueryLevel() {
        return tableNode.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {

        String selectString = "";
        boolean allOldName = true;
        for (int i = 0; i < tableNode.getSchema().size(); i ++) {
            String oldSchemaEntry =  tableNode.getSchema().get(i);
            String newSchemaEntry = this.newFieldNames.get(i);
            if (i != 0)
                selectString += ", ";
            String oldShortName = oldSchemaEntry.substring(oldSchemaEntry.lastIndexOf(".") + 1);
            if (oldShortName.equals(newSchemaEntry))
                selectString += oldSchemaEntry;
            else {
                selectString += oldSchemaEntry + " As " + newSchemaEntry;
                allOldName = false;
            }
        }

        String result = "";
        if (allOldName) {
            result = tableNode.prettyPrint(1, true).trim() + " As " + this.newTableName;
        } else {
            result = "(Select " + selectString + "\r\n" + "From "
                    + tableNode.prettyPrint(1, true).trim() + ") As " + this.newTableName;
        }

        if (asSubquery)
            return IndentationManager.addIndention(result, indentLv);
        return IndentationManager.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        return tableNode.getAllHoles();
    }

    @Override
    public AbsTableNode instantiate(InstantiateEnv env) {
        return new AbsRenameNode(
                this.newTableName,
                this.newFieldNames,
                this.tableNode.instantiate(env),
                this.renameTable,
                this.renameFields);
    }

    @Override
    public AbsTableNode substNamedVal(ValNodeSubstitution vnsb) {
        return new AbsRenameNode(newTableName, newFieldNames,
                this.tableNode.substNamedVal(vnsb), this.renameTable, this.renameFields);
    }

    @Override
    public List<AbsNamedTable> namedTableInvolved() {
        return tableNode.namedTableInvolved();
    }

    @Override
    public AbsTableNode tableSubst(List<Pair<AbsTableNode,AbsTableNode>> pairs) {
        return new AbsRenameNode(
                newTableName,
                newFieldNames,
                tableNode.tableSubst(pairs),
                renameTable,
                renameFields);
    }

    @Override
    public List<String> originalColumnName() {
        return this.tableNode.originalColumnName();
    }

    @Override
    public List<String> getSchema() {
        if (this.newTableName.equals("anonymous"))
            return this.newFieldNames;
        else {
            // add the qualifier
            return this.newFieldNames.stream()
                    .map(s -> this.newTableName + "." + s).collect(Collectors.toList());
        }
    }

    public AbsTableNode getTableNode() { return this.tableNode; }

    @Override
    public List<Value> getAllConstants() {
        return tableNode.getAllConstants();
    }

    @Override
    public String getQuerySkeleton() {
        return this.tableNode.getQuerySkeleton();
    }

}
