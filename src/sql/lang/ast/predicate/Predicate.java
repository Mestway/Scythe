package sql.lang.ast.predicate;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.val.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.transformation.ValNodeSubstitution;

import java.util.List;

/**
 * Created by clwang on 12/14/15.
 */
public interface Predicate {

    // evaluate the predicate with given environment
    boolean eval(Environment env) throws SQLEvalException;

    // for calculating the length of the eval
    int getPredLength();
    int getNestedQueryLevel();

    String prettyPrint(int indentLv);

    // check whether a eval has redundant components given the presence of f, examples are:
    // there exists a BinopPred that is identical to f
    // there exists a BinopPred that has same value but contrary operator
    boolean containRedundancy(Predicate p);
    List<Hole> getAllHoles();
    List<Value> getAllConstants();
    Predicate instantiate(InstantiateEnv env);
    Predicate substNamedVal(ValNodeSubstitution vnsb);
}
