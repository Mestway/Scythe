package sql.lang.ast.val;

import forward_enumeration.context.EnumContext;
import forward_enumeration.parameterized.InstantiateEnv;
import sql.lang.datatype.StringVal;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.ast.table.TableNode;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.List;

/**
 * Cast a 1X1 table into a value
 * Created by clwang on 12/17/15.
 */
public class TableAsVal implements ValNode {
    String name;
    TableNode tableNode;

    public TableAsVal(TableNode tn, String name) {
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
        String result = "(\r\n" + this.tableNode.prettyPrint(0) + ") AS " + this.name;
        return IndentionManagement.addIndention(result, lv);
    }

    @Override
    public int getNestedQueryLevel() {
        return this.tableNode.getNestedQueryLevel();
    }

    @Override
    public boolean equalsToValNode(ValNode vn) {
        if (vn instanceof TableAsVal)
            return true;//return tableNode.equalsToTableNode(((TableAsVal) vn).tableNode);
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return this.tableNode.getAllHoles();
    }

    @Override
    public ValNode instantiate(InstantiateEnv env) {
        return new TableAsVal(this.tableNode.instantiate(env), name);
    }

    @Override
    public ValNode subst(ValNodeSubstBinding vnsb) {
        return new TableAsVal(this.tableNode.substNamedVal(vnsb), name);
    }
}
