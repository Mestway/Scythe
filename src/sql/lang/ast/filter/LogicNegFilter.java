package sql.lang.ast.filter;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.List;

/**
 * Created by clwang on 12/23/15.
 */
public class LogicNegFilter implements Filter {

    Filter filter;

    public LogicNegFilter(Filter f) {
        this.filter = f;
    }

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        return ! filter.filter(env);
    }

    @Override
    public int getFilterLength() {
        return filter.getFilterLength();
    }

    @Override
    public int getNestedQueryLevel() {
        return filter.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        return IndentionManagement.addIndention("NOT " + filter.prettyPrint(0), indentLv);
    }

    @Override
    public boolean containsExclusiveFilter(Filter f) {
        return filter.containsExclusiveFilter(f);
    }

    @Override
    public List<Hole> getAllHoles() {
        return this.filter.getAllHoles();
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return new LogicNegFilter(this.filter.instantiate(env));
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return filter.substNamedVal(vnsb);
    }

}
