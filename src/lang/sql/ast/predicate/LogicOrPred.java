package lang.sql.ast.predicate;

import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import lang.sql.dataval.Value;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import util.IndentationManager;

import java.util.List;

/**
 * Created by clwang on 12/20/15.
 */
public class LogicOrPred implements Predicate {

    Predicate p1;
    Predicate p2;

    public LogicOrPred(Predicate p1, Predicate p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public boolean eval(Environment env) throws SQLEvalException {
        return p1.eval(env) || p2.eval(env);
    }

    @Override
    public int getPredLength() {
        return p1.getPredLength() + p2.getPredLength();
    }

    @Override
    public int getNestedQueryLevel() {
        return p1.getNestedQueryLevel() > p2.getNestedQueryLevel()?
                p1.getNestedQueryLevel() : p2.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        if (p1 instanceof EmptyPred || p2 instanceof EmptyPred) {
            return new EmptyPred().prettyPrint(indentLv);
        }
        String result = p1.prettyPrint(0) + "\r\n Or " + p2.prettyPrint(0);
        return IndentationManager.addIndention(result, indentLv);
    }

    @Override
    public boolean containRedundancy(Predicate f) {
        return false;
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
        return new LogicOrPred(p1.instantiate(env), p2.instantiate(env));
    }

    public static Predicate connectByOr(List<Predicate> filters) {
        Predicate last = filters.get(0);
        for (int i = 1; i < filters.size(); i ++) {
            last = new LogicOrPred(last, filters.get(i));
        }
        return last;
    }

    @Override
    public Predicate substNamedVal(ValNodeSubstitution vnsb) {
        return new LogicOrPred(p1.substNamedVal(vnsb), p2.substNamedVal(vnsb));
    }
}
