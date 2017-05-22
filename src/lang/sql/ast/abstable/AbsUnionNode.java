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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 10/22/16.
 */
public class AbsUnionNode extends AbsTableNode {

    List<AbsTableNode> tableNodes = new ArrayList<AbsTableNode>();

    public AbsUnionNode(List<AbsTableNode> tableNodes) {
        super();
        this.tableNodes = tableNodes;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        List<Table> tables = new ArrayList<Table>();
        for (AbsTableNode tn : tableNodes) {
            tables.add(tn.eval(env));
        }
        Table currentTable = tables.get(0);
        for (int i = 1; i < tables.size(); i ++) {
            currentTable = Table.union(currentTable, tables.get(i));
        }
        return currentTable;
    }

    @Override
    public List<String> getSchema() {
        return tableNodes.get(0).getSchema();
    }

    @Override
    public String getTableName() {
        return "anonymous";
    }

    @Override
    public List<ValType> getSchemaType() {
        return tableNodes.get(0).getSchemaType();
    }

    @Override
    // the level of join is the maximum table level + 1
    public int getNestedQueryLevel() {
        int lv = 0;
        for (AbsTableNode tn : this.tableNodes) {
            if (tn.getNestedQueryLevel() > lv)
                lv = tn.getNestedQueryLevel();
        }
        return lv + 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        String result = "Select * From \r\n" + this.tableNodes.get(0).prettyPrint(1, true).trim();
        for (int i = 1; i < this.tableNodes.size(); i ++) {
            result += "\r\nUnion All \r\n " + "Select * From\r\n" + this.tableNodes.get(i).prettyPrint(1, true);
        }
        return IndentationManager.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        this.tableNodes.forEach(tn -> result.addAll(tn.getAllHoles()));
        return result;
    }

    @Override
    public AbsTableNode instantiate(InstantiateEnv env) {
        return new AbsUnionNode(
                this.tableNodes.stream()
                        .map(t -> t.instantiate(env)).collect(Collectors.toList()));
    }

    @Override
    public AbsTableNode substNamedVal(ValNodeSubstitution vnsb) {
        return new AbsUnionNode(
                this.tableNodes.stream()
                        .map(t -> t.substNamedVal(vnsb)).collect(Collectors.toList()));    }

    @Override
    public List<AbsNamedTable> namedTableInvolved() {
        List<AbsNamedTable> result = new ArrayList<>();
        for (AbsTableNode t : this.tableNodes) {
            result.addAll(t.namedTableInvolved());
        }
        return result;
    }

    @Override
    public AbsTableNode tableSubst(List<Pair<AbsTableNode,AbsTableNode>> pairs) {
        return new AbsUnionNode(
                tableNodes.stream()
                        .map(tn -> tn.tableSubst(pairs))
                        .collect(Collectors.toList()));
    }

    @Override
    public List<String> originalColumnName() {
        return this.tableNodes.get(0).originalColumnName();
    }

    public List<AbsTableNode> getTableNodes() { return this.tableNodes; }

    @Override
    public String getQuerySkeleton() {
        return "(U" + tableNodes.stream()
                                .map(tn -> tn.getQuerySkeleton())
                                .reduce("", (x,y)-> (x + " " + y)) + ")";
    }

    @Override
    public List<Value> getAllConstants() {
        List<Value> result = new ArrayList<>();
        for (AbsTableNode tn : tableNodes) {
            result.addAll(tn.getAllConstants());
        }
        return result;
    }
}
