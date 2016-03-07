package constraint.lang.cval;

import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/5/16.
 */
public class CConstVal extends ConstraintVal {
    Value val;
    public CConstVal(Value v) {
        this.val = v;
    }

    @Override
    public Value eval(TableRow tr, Table t) {
        return val;
    }
}
