package constraint.lang.filter;

import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/7/16.
 */
public abstract class Parameter {
    public abstract Value instantiate(TableRow outTr, TableRow inTr);
    public abstract ValType getType(Table inT, Table outT);
}
