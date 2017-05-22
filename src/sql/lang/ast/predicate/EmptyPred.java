package sql.lang.ast.predicate;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.val.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.transformation.ValNodeSubstitution;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 1/4/16.
 */
public class EmptyPred implements Predicate {

    @Override
    public boolean eval(Environment env) throws SQLEvalException {
        return true;
    }

    @Override
    public int getPredLength() {
        return 1;
    }

    @Override
    public int getNestedQueryLevel() {
        return 0;
    }

    @Override
    public String prettyPrint(int indentLv) {
        return "";
    }

    @Override
    public boolean containRedundancy(Predicate f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return new ArrayList<>();
    }

    @Override
    public List<Value> getAllConstants() {
        return new ArrayList<>();
    }

    @Override
    public Predicate instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public Predicate substNamedVal(ValNodeSubstitution vnsb) {
        return this;
    }

    @Override
    public int hashCode() { return 1; }

    @Override
    public boolean equals(Object obj) { return obj instanceof EmptyPred; }

}
