package sql.lang.ast.filter;

import forward_enumeration.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;

import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/10/16.
 * TODO: not yet implemented
 */
public class FilterHole implements Filter, Hole {
    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        return false;
    }

    @Override
    public int getFilterLength() {
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
    public boolean containsExclusiveFilter(Filter f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return Arrays.asList(this);
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return this;
    }

}
