package constraint.lang.filter;

import sql.lang.DataType.Value;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/7/16.
 */
public class ConstParameter extends Parameter {

    Value constant;

    public ConstParameter(Value constant) {
        this.constant = constant;
    }

    @Override
    public Value instantiate(TableRow outTr, TableRow inTr) {
        return constant;
    }
}
