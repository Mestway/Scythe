package sql.lang.ast.filter;

import sql.lang.DataType.DateVal;
import sql.lang.DataType.FloatVal;
import sql.lang.DataType.IntVal;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.val.ValNode;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Created by clwang on 12/14/15.
 * A comparator between two columns
 */
public class VVComparator implements Filter {

    List<ValNode> args = new ArrayList<ValNode>();
    BiFunction<Value, Value, Boolean> compareFunc;

    public VVComparator(List<ValNode> args, BiFunction func) {
        this.args = args;
        this.compareFunc = func;
    }

    @Override
    public boolean filter(Environment env) {
        // two arguments
        Value v1 = args.get(0).eval(env);
        Value v2 = args.get(1).eval(env);
        return compareFunc.apply(v1, v2);
    }

    public static BiFunction<Value, Value, Boolean> lt = (v1, v2) -> {
        if (v1 instanceof IntVal && v2 instanceof IntVal) {
            return ((IntVal)v1).getVal() < ((IntVal)v2).getVal();
        } else if (v1 instanceof FloatVal && v2 instanceof FloatVal) {
            return ((FloatVal)v1).getVal() < ((FloatVal)v2).getVal();
        } else if (v1 instanceof DateVal && v2 instanceof DateVal) {
            return ((DateVal)v1).getVal().compareTo(((DateVal)v2).getVal()) < 0 ;
        }
        System.err.println("[Error@VVComparator45] " + "Comparing between none-number value");
        return false;
     };

    public static BiFunction<Value, Value, Boolean> eq = (v1, v2) -> {
        return v1.getVal().equals(v2.getVal());
    };

    public static BiFunction<Value, Value, Boolean> gt = (v1, v2) -> lt.apply(v2, v1);

    public static BiFunction<Value, Value, Boolean> le = (v1, v2) -> ! gt.apply(v1, v2);

    public static BiFunction<Value, Value, Boolean> ge = (v1, v2) -> ! lt.apply(v1, v2);

}
