package sql.lang.ast.filter;

import enumerator.parameterized.InstantiateEnv;
import sql.lang.DataType.DateVal;
import sql.lang.DataType.NumberVal;
import sql.lang.DataType.Value;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.ast.val.ValNode;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/14/15.
 * A comparator between two columns
 */
public class VVComparator implements Filter {

    List<ValNode> args = new ArrayList<>();
    BiFunction<Value, Value, Boolean> compareFunc;

    public VVComparator(List<ValNode> args, BiFunction func) {
        this.args = args;
        this.compareFunc = func;
    }

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        // two arguments
        Value v1 = args.get(0).eval(env);
        Value v2 = args.get(1).eval(env);
        return compareFunc.apply(v1, v2);
    }

    @Override
    public int getFilterLength() {
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
                + OperatorName(this.compareFunc)
                + " "
                + args.get(1).prettyPrint(indentLv + 1).trim();
        return IndentionManagement.addIndention(result, indentLv);
    }

    @Override
    public boolean containsExclusiveFilter(Filter f) {
        if (f instanceof VVComparator) {
            boolean exclusive = true;
            for (ValNode v : this.args) {
                if (! ((VVComparator) f).args.contains(v))
                    return false;
            }
            for (ValNode v : ((VVComparator) f).args) {
                if (! this.args.contains(v))
                    return false;
            }
            return exclusive;
        }
        return f.containsExclusiveFilter(this);
    }

    public static BiFunction<Value, Value, Boolean> lt = (v1, v2) -> {
        if (v1 instanceof NumberVal && v2 instanceof NumberVal) {
            return ((NumberVal)v1).getVal() < ((NumberVal)v2).getVal();
        } else if (v1 instanceof DateVal && v2 instanceof DateVal) {
            return ((DateVal)v1).getVal().compareTo(((DateVal)v2).getVal()) < 0 ;
        }
        System.out.println("[Error@VVComparator45] " + "Comparing between none-number value: " + v1.toString() + " and " + v2.toString());
        return false;
     };

    public static BiFunction<Value, Value, Boolean> eq = (v1, v2) -> {
        return v1.getVal().equals(v2.getVal());
    };

    public static BiFunction<Value, Value, Boolean> neq = (v1, v2) -> ! eq.apply(v1, v2);

    public static BiFunction<Value, Value, Boolean> gt = (v1, v2) -> lt.apply(v2, v1);

    public static BiFunction<Value, Value, Boolean> le = (v1, v2) -> ! gt.apply(v1, v2);

    public static BiFunction<Value, Value, Boolean> ge = (v1, v2) -> ! lt.apply(v1, v2);

    public static List<BiFunction<Value, Value, Boolean>> getAllFunctions() {
        return Arrays.asList(lt, eq, gt, le, ge, neq);
    }

    private String OperatorName(BiFunction<Value, Value, Boolean> op) {
        if (op.equals(eq)) return "==";
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
    public Filter instantiate(InstantiateEnv env) {
        return new VVComparator(
                args.stream().map(vn -> vn.instantiate(env)).collect(Collectors.toList()),
                this.compareFunc);
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        Filter f = new VVComparator(args.stream().map(vn -> vn.subst(vnsb)).collect(Collectors.toList()), this.compareFunc);
        return f;
    }

    public List<ValNode> getArgs() { return this.args; }
    public BiFunction<Value, Value, Boolean> getComparator() { return this.compareFunc; }

}
