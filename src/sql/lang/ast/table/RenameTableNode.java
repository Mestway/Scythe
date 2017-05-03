package sql.lang.ast.table;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.datatype.Value;
import util.Pair;
import sql.lang.datatype.ValType;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/21/15.
 */
public class RenameTableNode extends TableNode {

    String newTableName;
    List<String> newFieldNames;
    TableNode tableNode;

    public RenameTableNode(String tableName, List<String> fieldsName, TableNode tn) {
        this.newTableName = tableName;
        this.newFieldNames = fieldsName;
        this.tableNode = tn;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        Table table = tableNode.eval(env);
        table.updateName(this.newTableName);
        table.updateSchema(this.newFieldNames);
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
    public int getASTNodeCnt() {
        return this.tableNode.getASTNodeCnt();
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
            return IndentionManagement.addIndention(result, indentLv);
        return IndentionManagement.addIndention(result, indentLv);
    }

    public String ppWithPartialIndex(List<String> columnSubset, int indentLv, Filter predicate, boolean asSubquery) {

        String selectString = "";

        Map<String, String> schemaToBetterOnes = new HashMap<>();

        ValNodeSubstBinding vnsb = new ValNodeSubstBinding();

        for (int i = 0; i < tableNode.getSchema().size(); i ++) {

            String oldSchemaEntry = tableNode.getSchema().get(i);
            String newSchemaEntry = this.newFieldNames.get(i);

            vnsb.addBinding(new Pair<>(new NamedVal(this.newTableName + "." + newSchemaEntry), new NamedVal(newSchemaEntry)));

            String oldShortName = oldSchemaEntry.substring(oldSchemaEntry.lastIndexOf(".") + 1);

            if (oldShortName.equals(newSchemaEntry))
                schemaToBetterOnes.put(this.getSchema().get(i), oldSchemaEntry);
            else {
                schemaToBetterOnes.put(this.getSchema().get(i), oldSchemaEntry + " As " + newSchemaEntry);
            }
        }

        boolean noEntryYet = true;
        for (String s : columnSubset) {
            if (! noEntryYet) selectString += ", ";
            noEntryYet = false;
            selectString += schemaToBetterOnes.get(s);
        }


        String result = "Select " + selectString + "\r\n" + "From "
                    + tableNode.prettyPrint(1, false).trim();

        if (! (predicate instanceof EmptyFilter)) {
            result += "\r\n Where " + predicate.substNamedVal(vnsb).prettyPrint(0);
        }

        if (asSubquery)
            return IndentionManagement.addIndention("(" + result + ")", indentLv);
        else return IndentionManagement.addIndention(result , indentLv);
    }

    @Override
    public TableNode simplifyAST() {
        TableNode simplifiedCore = this.tableNode.simplifyAST();
        if (simplifiedCore instanceof RenameTableNode) {
            return new RenameTableNode(this.newTableName, this.newFieldNames,
                    ((RenameTableNode) simplifiedCore).getTableNode()).simplifyAST();
        } else {
            return new RenameTableNode(this.newTableName, this.newFieldNames, simplifiedCore);
        }
    }

    @Override
    public List<Hole> getAllHoles() {
        return tableNode.getAllHoles();
    }

    @Override
    public TableNode instantiate(InstantiateEnv env) {
        return new RenameTableNode(
                this.newTableName,
                this.newFieldNames,
                this.tableNode.instantiate(env));
    }

    @Override
    public TableNode substNamedVal(ValNodeSubstBinding vnsb) {
        return new RenameTableNode(newTableName, newFieldNames,
                this.tableNode.substNamedVal(vnsb));
    }

    @Override
    public List<NamedTable> namedTableInvolved() {
        return tableNode.namedTableInvolved();
    }

    @Override
    public TableNode tableSubst(List<Pair<TableNode,TableNode>> pairs) {
        return new RenameTableNode(
                newTableName,
                newFieldNames,
                tableNode.tableSubst(pairs));
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

    public TableNode getTableNode() { return this.tableNode; }

    @Override
    public double estimateAllFilterCost() {
        return this.tableNode.estimateAllFilterCost();
    }

    @Override
    public List<Value> getAllConstants() {
        return tableNode.getAllConstants();
    }

    @Override
    public String getQuerySkeleton() {
        return this.tableNode.getQuerySkeleton();
    }
}
