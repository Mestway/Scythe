package sql.lang.ast.filter;

import enumerator.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.List;

/**
 * Created by clwang on 12/20/15.
 */
public class LogicAndFilter implements Filter {

    Filter f1;
    Filter f2;

    public LogicAndFilter(Filter f1, Filter f2) {
        this.f1 = f1;
        this.f2 = f2;
    }

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        return f1.filter(env) && f2.filter(env);
    }

    @Override
    public int getFilterLength() { return f1.getFilterLength() + f2.getFilterLength(); }

    @Override
    public int getNestedQueryLevel() {
        return f1.getNestedQueryLevel() > f2.getNestedQueryLevel() ?
                f1.getNestedQueryLevel() : f2.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        if (f1 instanceof EmptyFilter)
            return IndentionManagement.addIndention(f2.prettyPrint(0), indentLv);
        else if (f2 instanceof EmptyFilter)
            return IndentionManagement.addIndention(f1.prettyPrint(0), indentLv);
        else {
            String result = f1.prettyPrint(0) + "\r\n AND " + f2.prettyPrint(0);
            return IndentionManagement.addIndention(result, indentLv);
        }
    }

    @Override
    public boolean containsExclusiveFilter(Filter f) {
        return f1.containsExclusiveFilter(f) && f2.containsExclusiveFilter(f);
    }

    public static Filter connectByAnd(List<Filter> filters) {
        Filter last = filters.get(0);
        for (int i = 1; i < filters.size(); i ++) {
            last = new LogicAndFilter(last, filters.get(i));
        }
        return last;
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = f1.getAllHoles();
        result.addAll(f2.getAllHoles());
        return result;
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return new LogicAndFilter(f1.instantiate(env), f2.instantiate(env));
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return new LogicAndFilter(f1.substNamedVal(vnsb), f2.substNamedVal(vnsb));
    }

}
