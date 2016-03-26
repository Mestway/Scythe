package invariant.lang.filter;

import sql.lang.DataType.Value;
import sql.lang.TableRow;
import sql.lang.ast.filter.VVComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by clwang on 3/6/16.
 */
public class CBiComparator {
    Parameter left, right;
    BiFunction<Value, Value, Boolean> compareFunc;

    public CBiComparator(Parameter left,
                         Parameter right,
                         BiFunction<Value, Value, Boolean> compareFunc) {
        this.left = left;
        this.right = right;
        this.compareFunc = compareFunc;
    }

    public boolean compare(TableRow outTr, TableRow inTr) {
        return compareFunc.apply(left.instantiate(outTr, inTr), right.instantiate(outTr, inTr));
    }

    public String toString() {
        return left.toString() + comparatorToString(compareFunc) + right.toString();
    }

    public static String comparatorToString(BiFunction<Value, Value, Boolean> c) {
        if (c.equals(VVComparator.eq)) return "==";
        else if (c.equals(VVComparator.ge)) return ">=";
        else if (c.equals(VVComparator.gt)) return ">";
        else if (c.equals(VVComparator.le)) return "<=";
        else if (c.equals(VVComparator.lt)) return "<";
        else if (c.equals(VVComparator.neq)) return "<>";
        else return "*";
    }

    public static boolean conflict(CBiComparator c1, CBiComparator c2) {
        if (c1.left.equals(c2.left) && c1.right.equals(c2.right))
            return true;
        if (c1.left.equals(c2.right) && c1.right.equals(c2.left))
            return true;
        return false;
    }

}
