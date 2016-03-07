package constraint.lang.cval;

import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by clwang on 3/5/16.
 */
public abstract class ConstraintVal {
    public abstract Value eval(TableRow outputRow, Table inputTable);
}
