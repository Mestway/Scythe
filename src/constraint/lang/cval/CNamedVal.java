package constraint.lang.cval;

import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/5/16.
 */
public class CNamedVal extends ConstraintVal {
    String name;

    public CNamedVal(String name) { this.name = name; }

    @Override
    public Value eval(TableRow outTr, Table inputTable) {
        return outTr.getValue(outTr.retrieveIndex(name));
    }
}
