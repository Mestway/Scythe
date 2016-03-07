package constraint.lang.exp;

import sql.lang.Table;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/6/16.
 */
public abstract class ConstraintExp {
    public abstract boolean eval(TableRow outputTR, Table input);
}
