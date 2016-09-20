package sql.lang.ast.val;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.ArrayList;
import java.util.List;

/**
 * A value which is a constant
 * Created by clwang on 12/16/15.
 */
public class ConstantVal implements ValNode {
    Value val;

    public ConstantVal(Value val) {
        this.val = val;
    }

    public void setVal(Value val) {
        this.val = val;
    }

    public Value eval(Environment env) throws SQLEvalException {
        return val;
    }

    @Override
    public String getName() {
        return "anonymous";
    }

    @Override
    public ValType getType(EnumContext ctxt) {
        return val.getValType();
    }

    @Override
    public String prettyPrint(int lv) {
        return IndentionManagement.addIndention(val.toString(), lv);
    }

    @Override
    public int getNestedQueryLevel() {
        return 0;
    }

    @Override
    public boolean equalsToValNode(ValNode vn) {
        if (vn instanceof ConstantVal) {
            if (this.val.equals(((ConstantVal) vn).val))
                return true;
        }
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return new ArrayList<>();
    }

    @Override
    public ValNode instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public ValNode subst(ValNodeSubstBinding vnsb) {
        return this;
    }
}
