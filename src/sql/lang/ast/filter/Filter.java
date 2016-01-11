package sql.lang.ast.filter;

import enumerator.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;

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
    //boolean equalsToFilter(Filter f);

    // we also consider same as "exclusive"
    boolean containsExclusiveFilter(Filter f);
    List<Hole> getAllHoles();
    Filter instantiate(InstantiateEnv env);
}
