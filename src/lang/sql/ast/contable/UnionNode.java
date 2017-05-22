package lang.sql.ast.contable;

import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import util.IndentationManager;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 10/22/16.
 */
public class UnionNode extends TableNode {

    List<TableNode> tableNodes = new ArrayList<TableNode>();

    public UnionNode(List<TableNode> tableNodes) {
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
        for (TableNode tn : this.tableNodes) {
            if (tn.getNestedQueryLevel() > lv)
                lv = tn.getNestedQueryLevel();
        }
        return lv + 1;
    }

    @Override
    public int getASTNodeCnt() {
        return this.tableNodes.stream().map(tn -> tn.getASTNodeCnt()).reduce(0, (x,y)->(x+y)) + 1;
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
    public TableNode simplifyAST() {
        return new UnionNode(this.tableNodes.stream().map(tn -> tn.simplifyAST()).collect(Collectors.toList()));
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        this.tableNodes.forEach(tn -> result.addAll(tn.getAllHoles()));
        return result;
    }

    @Override
    public TableNode instantiate(InstantiateEnv env) {
        return new UnionNode(
                this.tableNodes.stream()
                        .map(t -> t.instantiate(env)).collect(Collectors.toList()));
    }

    @Override
    public TableNode substNamedVal(ValNodeSubstitution vnsb) {
        return new UnionNode(
                this.tableNodes.stream()
                        .map(t -> t.substNamedVal(vnsb)).collect(Collectors.toList()));    }

    @Override
    public List<NamedTableNode> namedTableInvolved() {
        List<NamedTableNode> result = new ArrayList<>();
        for (TableNode t : this.tableNodes) {
            result.addAll(t.namedTableInvolved());
        }
        return result;
    }

    @Override
    public TableNode tableSubst(List<Pair<TableNode,TableNode>> pairs) {
        return new UnionNode(
                tableNodes.stream()
                        .map(tn -> tn.tableSubst(pairs))
                        .collect(Collectors.toList()));
    }

    @Override
    public List<String> originalColumnName() {
        return this.tableNodes.get(0).originalColumnName();
    }

    public List<TableNode> getTableNodes() { return this.tableNodes; }

    @Override
    public double estimateAllFilterCost() {
        return tableNodes.stream().map(tn -> tn.estimateAllFilterCost()).reduce(0., (x,y) -> (x + y));
    }

    @Override
    public String getQuerySkeleton() {
        return "(U" + tableNodes.stream().map(tn -> tn.getQuerySkeleton()).reduce("", (x,y)-> (x + " " + y)) + ")";
    }

    @Override
    public List<Value> getAllConstants() {
        List<Value> result = new ArrayList<>();
        for (TableNode tn : tableNodes) {
            result.addAll(tn.getAllConstants());
        }
        return result;
    }
}
