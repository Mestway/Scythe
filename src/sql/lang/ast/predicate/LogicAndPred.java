package sql.lang.ast.predicate;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.val.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.transformation.ValNodeSubstitution;
import util.IndentationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 12/20/15.
 */
public class LogicAndPred implements Predicate {

    Predicate p1;
    Predicate p2;

    public LogicAndPred(Predicate p1, Predicate p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public boolean eval(Environment env) throws SQLEvalException {
        return p1.eval(env) && p2.eval(env);
    }

    @Override
    public int getPredLength() { return p1.getPredLength() + p2.getPredLength(); }

    @Override
    public int getNestedQueryLevel() {
        return p1.getNestedQueryLevel() > p2.getNestedQueryLevel() ?
                p1.getNestedQueryLevel() : p2.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        if (p1 instanceof EmptyPred)
            return IndentationManager.addIndention(p2.prettyPrint(0), indentLv);
        else if (p2 instanceof EmptyPred)
            return IndentationManager.addIndention(p1.prettyPrint(0), indentLv);
        else {
            String result = p1.prettyPrint(0) + "\r\n And " + p2.prettyPrint(0);
            return IndentationManager.addIndention(result, indentLv);
        }
    }

    @Override
    public boolean containRedundancy(Predicate f) {
        return p1.containRedundancy(f) && p2.containRedundancy(f);
    }

    public static Predicate connectByAnd(List<Predicate> filters) {
        Predicate last = filters.get(0);
        for (int i = 1; i < filters.size(); i ++) {
            last = new LogicAndPred(last, filters.get(i));
        }
        return last;
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = p1.getAllHoles();
        result.addAll(p2.getAllHoles());
        return result;
    }

    @Override
    public List<Value> getAllConstants() {
        List<Value> list =  p1.getAllConstants();
        list.addAll(p2.getAllConstants());
        return list;
    }

    @Override
    public Predicate instantiate(InstantiateEnv env) {
        return new LogicAndPred(p1.instantiate(env), p2.instantiate(env));
    }

    @Override
    public Predicate substNamedVal(ValNodeSubstitution vnsb) {
        return new LogicAndPred(p1.substNamedVal(vnsb), p2.substNamedVal(vnsb));
    }

    public List<Predicate> getAllFilters() {
        List<Predicate> result = new ArrayList<>();
        if (p1 instanceof LogicAndPred) {
            result.addAll(((LogicAndPred) p1).getAllFilters());
        } else {
            result.add(p1);
        }

        if (p2 instanceof LogicAndPred) {
            result.addAll(((LogicAndPred) p2).getAllFilters());
        } else {
            result.add(p2);
        }

        return result;
    }

}
