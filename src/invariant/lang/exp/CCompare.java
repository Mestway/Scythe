package invariant.lang.exp;

import invariant.lang.cval.ConstraintVal;
import enumerator.Constraint;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;

import java.util.function.BiFunction;

/**
 * Created by clwang on 3/6/16.
 */
public class CCompare extends ConstraintExp {

    ConstraintVal left;
    ConstraintVal right;
    BiFunction<Value, Value, Boolean> compareFunc;

    public CCompare(ConstraintVal left,
                    ConstraintVal right,
                    BiFunction<Value, Value, Boolean> cmp) {
        this.left = left;
        this.right = right;
        this.compareFunc = cmp;
    }

    @Override
    public boolean eval(TableRow outputTR, Table input) {
        return compareFunc.apply(left.eval(outputTR, input), right.eval(outputTR, input));
    }
}
