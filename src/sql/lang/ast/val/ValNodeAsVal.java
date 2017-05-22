package sql.lang.ast.val;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.val.ValType;
import sql.lang.val.Value;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.transformation.ValNodeSubstitution;

import java.util.List;

/**
 * Cast a constant value to a name
 * Created by clwang on 12/17/15.
 */
public class ValNodeAsVal implements ValNode {

    ValNode valNode;
    String name;

    public ValNodeAsVal(ValNode valNode, String name) {
        this.valNode = valNode;
        this.name = name;
    }

    @Override
    public Value eval(Environment env) throws SQLEvalException {
        return valNode.eval(env);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ValType getType(EnumContext ctxt) {
        return valNode.getType(ctxt);
    }

    @Override
    public String prettyPrint(int Lv) {
        return valNode.toString() + " AS " + name;
    }

    @Override
    public int getNestedQueryLevel() {
        return valNode.getNestedQueryLevel();
    }

    @Override
    public boolean equalsToValNode(ValNode vn) {
        if (vn instanceof ValNodeAsVal) {
            if (((ValNodeAsVal) vn).valNode.equalsToValNode(valNode))
                return true;
        }
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return valNode.getAllHoles();
    }

    @Override
    public ValNode instantiate(InstantiateEnv env) {
        return new ValNodeAsVal(valNode.instantiate(env), this.name);
    }

    @Override
    public ValNode subst(ValNodeSubstitution vnsb) {
        return new ValNodeAsVal(valNode.subst(vnsb), this.name);
    }

}
