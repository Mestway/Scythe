package lang.sql.ast.contable;

import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import util.Pair;
import util.IndentationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/18/15.
 * Join is implemented as cartesian product
 */
public class JoinNode extends TableNode {

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
            currentTable = Table.joinTwo(currentTable, tables.get(i));
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
    public int getASTNodeCnt() {
        return 1 + this.tableNodes.stream().map(tn -> tn.getASTNodeCnt()).reduce(0, (x,y)->x + y);
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        String result = this.tableNodes.get(0).prettyPrint(1, true).trim();
        for (int i = 1; i < this.tableNodes.size(); i ++) {
            result += " Join \r\n" + this.tableNodes.get(i).prettyPrint(1,true);
        }
        if (asSubquery) {
            result = "(" + result + ")";
        }
        return IndentationManager.addIndention(result, indentLv);
    }

    @Override
    public TableNode simplifyAST() {
        return new JoinNode(this.tableNodes.stream().map(tn -> tn.simplifyAST()).collect(Collectors.toList()));
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

    @Override
    public TableNode substNamedVal(ValNodeSubstitution vnsb) {
        return new JoinNode(
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
        return new JoinNode(
                tableNodes.stream()
                        .map(tn -> tn.tableSubst(pairs))
                        .collect(Collectors.toList()));
    }

    @Override
    public List<String> originalColumnName() {
        List<String> result = new ArrayList<>();
        for (TableNode tn : this.tableNodes) {
            result.addAll(tn.originalColumnName());
        }
        return result;
    }

    public List<TableNode> getTableNodes() { return this.tableNodes; }

    @Override
    public double estimateAllFilterCost() {
        return tableNodes.stream().map(tn -> tn.estimateAllFilterCost()).reduce(0., (x,y) -> (x + y));
    }

    @Override
    public List<Value> getAllConstants() {
        List<Value> result = new ArrayList<>();
        for (TableNode tn : tableNodes) {
            result.addAll(tn.getAllConstants());
        }

        return result;
    }

    @Override
    public String getQuerySkeleton() {
        return "(J" + tableNodes.stream().map(tn -> tn.getQuerySkeleton()).reduce("", (x,y)-> (x + " " + y)) + ")";
    }

    public List<Table> getNamedTableInJoin() {
        List<Table> result = new ArrayList<>();
        for (TableNode tn : this.tableNodes) {
            if (tn instanceof NamedTableNode)
                result.add(((NamedTableNode) tn).getTable());
            if (tn instanceof JoinNode) {
                result.addAll(((JoinNode) tn).getNamedTableInJoin());
            }
        }
        return result;
    }

}
