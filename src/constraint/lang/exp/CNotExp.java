package constraint.lang.exp;

import sql.lang.Table;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/6/16.
 */
public class CNotExp extends ConstraintExp {
    ConstraintExp cexp;
    public CNotExp(ConstraintExp ce) {
        this.cexp = ce;
    }

    @Override
    public boolean eval(TableRow outputTR, Table input) {
        return ! cexp.eval(outputTR, input);
    }
}
