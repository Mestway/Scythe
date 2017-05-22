package lang.sql.ast.valnode;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.table.Table;
import lang.sql.ast.Hole;
import lang.sql.ast.contable.TableNode;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.sql.dataval.StringVal;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import lang.sql.ast.Environment;
import util.IndentationManager;

import java.util.List;

/**
 * Cast a 1X1 table into a value
 * Created by clwang on 12/17/15.
 */
public class TableNodeAsVal implements ValNode {

    String name;
    TableNode tableNode;

    public TableNodeAsVal(TableNode tn, String name) {
        this.tableNode = tn;
        this.name = name;
    }

    @Override
    public Value eval(Environment env) throws SQLEvalException {
        Table table = tableNode.eval(env);
        Value val = null;

        if (table.getContent().size() == 0) {
            return new StringVal("empty");
        }

        try {
            val = table.tableToValue();
        } catch (Exception e) {
            throw new SQLEvalException("Table to val exception");
        }

        return val;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public ValType getType(EnumContext ctxt) {
        return tableNode.getSchemaType().get(0);
    }

    @Override
    public String prettyPrint(int lv) {
        String result = "(\r\n" + this.tableNode.prettyPrint(0, true) + ") As " + this.name;
        return IndentationManager.addIndention(result, lv);
    }

    @Override
    public int getNestedQueryLevel() {
        return this.tableNode.getNestedQueryLevel();
    }

    @Override
    public boolean equalsToValNode(ValNode vn) {
        if (vn instanceof TableNodeAsVal)
            return true;//return tableNode.equalsToTableNode(((TableAsVal) vn).tableNode);
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return this.tableNode.getAllHoles();
    }

    @Override
    public ValNode instantiate(InstantiateEnv env) {
        return new TableNodeAsVal(this.tableNode.instantiate(env), name);
    }

    @Override
    public ValNode subst(ValNodeSubstitution vnsb) {
        return new TableNodeAsVal(this.tableNode.substNamedVal(vnsb), name);
    }
}
