package sql.lang.ast.filter;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 1/4/16.
 */
public class EmptyFilter implements Filter {

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        return true;
    }

    @Override
    public int getFilterLength() {
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
    public boolean containRedundantFilter(Filter f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return new ArrayList<>();
    }

    @Override
    public List<Value> getAllConstatnts() {
        return new ArrayList<>();
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return this;
    }

    @Override
    public int hashCode() { return 1; }

    @Override
    public boolean equals(Object obj) { return obj instanceof EmptyFilter; }

}
