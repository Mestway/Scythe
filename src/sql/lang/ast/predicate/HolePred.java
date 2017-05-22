package sql.lang.ast.predicate;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.val.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.transformation.ValNodeSubstitution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/10/16.
 * TODO: not yet implemented
 */
public class HolePred implements Predicate, Hole {
    @Override
    public boolean eval(Environment env) throws SQLEvalException {
        return false;
    }

    @Override
    public int getPredLength() {
        return 0;
    }

    @Override
    public int getNestedQueryLevel() {
        return 0;
    }

    @Override
    public String prettyPrint(int indentLv) {
        return null;
    }

    @Override
    public boolean containRedundancy(Predicate f) { return false; }

    @Override
    public List<Hole> getAllHoles() {
        return Arrays.asList(this);
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

}
