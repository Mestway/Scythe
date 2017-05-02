package sql.lang.ast.filter;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;

import java.util.List;

/**
 * Created by clwang on 12/14/15.
 */
public interface Filter {

    // for evaluation
    boolean filter(Environment env) throws SQLEvalException;

    // for calculating the length of the filter
    int getFilterLength();
    int getNestedQueryLevel();

    String prettyPrint(int indentLv);

    // check whether a filter has redundant components given the presence of f, examples are:
    // there exists a BinopFilter that is identical to f
    // there exists a BinopFilter that has same value but contrary operator
    boolean containRedundantFilter(Filter f);
    List<Hole> getAllHoles();
    List<Value> getAllConstatnts();
    Filter instantiate(InstantiateEnv env);
    Filter substNamedVal(ValNodeSubstBinding vnsb);
}
