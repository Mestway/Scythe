package constraint.lang.filter;

import sql.lang.DataType.Value;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/7/16.
 */
public abstract class Parameter {
    public abstract Value instantiate(TableRow outTr, TableRow inTr);
}
