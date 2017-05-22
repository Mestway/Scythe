package lang.sql.ast.predicate;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.dataval.Value;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.ast.valnode.ValNode;
import lang.sql.dataval.NullVal;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import util.IndentationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 10/17/16.
 */
public class IsNullPred implements Predicate {

    // If this flag is set to true, the eval is actually IS NOT NULL xxx
    boolean negSign = false;
    // the column to be tested
    ValNode arg;

    public IsNullPred(ValNode arg, boolean negSign) {
        this.arg = arg;
        this.negSign = negSign;
    }

    @Override
    public boolean eval(Environment env) throws SQLEvalException {
        Value v1 = arg.eval(env);
        if (v1 instanceof NullVal)
            return ! negSign;
        return negSign;
    }

    @Override
    public int getPredLength() {
        return 1;
    }

    @Override
    public int getNestedQueryLevel() {
        return Math.max(arg.getNestedQueryLevel(), 0);
    }

    @Override
    public String prettyPrint(int indentLv) {
        String str = arg.prettyPrint(0) + " Is" + (negSign ? " Not " : " ") + "NULL";
        return IndentationManager.addIndention(str, indentLv);
    }

    @Override
    public boolean containRedundancy(Predicate f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return arg.getAllHoles();
    }

    @Override
    public List<Value> getAllConstants() {
        return new ArrayList<>();
    }

    @Override
    public Predicate instantiate(InstantiateEnv env) {
        return new IsNullPred(arg.instantiate(env), this.negSign);
    }

    @Override
    public Predicate substNamedVal(ValNodeSubstitution vnsb) {
        return new IsNullPred(arg.subst(vnsb), this.negSign);
    }
}
