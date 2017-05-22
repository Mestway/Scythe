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

/**
 * Created by clwang on 4/13/17.
 */
public class AbsProjNode extends AbsTableNode {

    AbsTableNode tableNode;

    public AbsProjNode(AbsTableNode atn) {
        super();
        this.tableNode = atn;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        return this.tableNode.eval(new Environment());
    }

    @Override
    public List<String> getSchema() {
        return this.tableNode.getSchema();
    }

    @Override
    public String getTableName() {
        return tableNode.getTableName();
    }

    @Override
    public List<ValType> getSchemaType() {
        return tableNode.getSchemaType();
    }

    @Override
    public int getNestedQueryLevel() {
        return this.tableNode.getNestedQueryLevel() + 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        String result = "Proj" + this.tableNode.prettyPrint(1, true).trim();
        return IndentationManager.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        return this.tableNode.getAllHoles();
    }

    @Override
    public AbsTableNode instantiate(InstantiateEnv env) {
        return this.tableNode.instantiate(env);
    }

    @Override
    public AbsTableNode substNamedVal(ValNodeSubstitution vnsb) {
        return this.tableNode.substNamedVal(vnsb);
    }

    @Override
    public List<AbsNamedTable> namedTableInvolved() {
        return this.tableNode.namedTableInvolved();
    }

    @Override
    public AbsTableNode tableSubst(List<Pair<AbsTableNode, AbsTableNode>> pairs) {
        return this.tableNode.tableSubst(pairs);
    }

    @Override
    public List<String> originalColumnName() {
        return this.tableNode.originalColumnName();
    }

    @Override
    public List<Value> getAllConstants() {
        return this.tableNode.getAllConstants();
    }

    @Override
    public String getQuerySkeleton() {
        return "(Proj " + this.tableNode.getQuerySkeleton() + ")";
    }
}
