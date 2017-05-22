package sql.lang.ast.predicate;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.val.ConstantVal;
import sql.lang.val.*;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.ast.val.ValNode;
import sql.lang.transformation.ValNodeSubstitution;
import util.IndentationManager;

import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/14/15.
 * A comparator between values
 */
public class BinopPred implements Predicate {

    List<ValNode> args = new ArrayList<>();
    BiFunction<Value, Value, Boolean> binop;

    public BinopPred(List<ValNode> args, BiFunction func) {
        this.args = args;
        this.binop = func;
    }

    @Override
    public boolean eval(Environment env) throws SQLEvalException {
        // two arguments
        Value v1 = args.get(0).eval(env);
        Value v2 = args.get(1).eval(env);
        return binop.apply(v1, v2);
    }

    @Override
    public int getPredLength() {
        return 1;
    }

    @Override
    public int getNestedQueryLevel() {
        int lv = 0;
        for (ValNode vn : this.args) {
            if (vn.getNestedQueryLevel() > lv)
                lv = vn.getNestedQueryLevel();
        }
        return lv;
    }

    @Override
    public String prettyPrint(int indentLv) {
        String result = args.get(0).prettyPrint(indentLv + 1).trim()
                + " "
                + OperatorName(this.binop)
                + " "
                + args.get(1).prettyPrint(indentLv + 1).trim();
        return IndentationManager.addIndention(result, indentLv);
    }

    @Override
    public boolean containRedundancy(Predicate f) {
        if (f instanceof BinopPred) {
            boolean exclusive = true;
            for (ValNode v : this.args) {
                if (! ((BinopPred) f).args.contains(v))
                    return false;
            }
            for (ValNode v : ((BinopPred) f).args) {
                if (! this.args.contains(v))
                    return false;
            }
            return exclusive;
        }
        return f.containRedundancy(this);
    }

    public static BiFunction<Value, Value, Boolean> lt = (v1, v2) -> {

        if (! v1.getValType().equals(v2.getValType())) {
            System.out.println("[Error@VVComparator45] "
                                + "Comparing between none-number value: "
                                + v1.toString() + " and " + v2.toString());
        }

        // TODO: double check this
        if (v1 instanceof NullVal || v2 instanceof NullVal)
            return false;

        if (v1 instanceof NumberVal && v2 instanceof NumberVal) {
            return ((NumberVal)v1).getVal() < ((NumberVal)v2).getVal();
        } else if (v1 instanceof DateVal && v2 instanceof DateVal) {
            return ((DateVal)v1).getVal().compareTo(((DateVal)v2).getVal()) < 0 ;
        }
        return false;
     };

    public static BiFunction<Value, Value, Boolean> eq = (v1, v2) -> {
        return v1.getVal().equals(v2.getVal());
    };

    public static BiFunction<Value, Value, Boolean> neq = (v1, v2) -> ! eq.apply(v1, v2);
    public static BiFunction<Value, Value, Boolean> gt = (v1, v2) -> lt.apply(v2, v1);
    public static BiFunction<Value, Value, Boolean> le = (v1, v2) -> (lt.apply(v1, v2) || eq.apply(v1, v2));
    public static BiFunction<Value, Value, Boolean> ge = (v1, v2) -> (gt.apply(v1, v2) || eq.apply(v1, v2));
    public static List<BiFunction<Value, Value, Boolean>> getAllFunctions() {
        return Arrays.asList(lt, eq, gt, le, ge, neq);
    }

    private String OperatorName(BiFunction<Value, Value, Boolean> op) {
        if (op.equals(eq)) return "=";
        else if (op.equals(le)) return "<=";
        else if (op.equals(ge)) return ">=";
        else if (op.equals(lt)) return "<";
        else if (op.equals(gt)) return ">";
        else if (op.equals(neq)) return "<>";
        else return "??";
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        this.args.forEach(vn -> result.addAll(vn.getAllHoles()));
        return result;
    }

    @Override
    public List<Value> getAllConstants() {
        List<Value> values = new ArrayList<>();
        for (ValNode vn : this.args) {
            if (vn instanceof ConstantVal) {
                values.add(((ConstantVal) vn).getValue());
            }
        }
        return values;
    }

    @Override
    public Predicate instantiate(InstantiateEnv env) {
        return new BinopPred(args.stream().map(vn -> vn.instantiate(env)).collect(Collectors.toList()), this.binop);
    }

    @Override
    public Predicate substNamedVal(ValNodeSubstitution vnsb) {
        Predicate f = new BinopPred(args.stream().map(vn -> vn.subst(vnsb)).collect(Collectors.toList()), this.binop);
        return f;
    }

    public List<ValNode> getArgs() { return this.args; }
    public BiFunction<Value, Value, Boolean> getComparator() { return this.binop; }

}
