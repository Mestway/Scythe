package constraint.lang.filter;

import sql.lang.DataType.Value;
import sql.lang.TableRow;

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

}
