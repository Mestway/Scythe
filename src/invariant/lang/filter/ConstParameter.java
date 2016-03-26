package invariant.lang.filter;

import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
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

    @Override
    public ValType getType(Table inT, Table outT) {
        return getValue().getValType();
    }

    @Override
    public String toString() {
        return constant.toString();
    }

    public Value getValue() { return constant; }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConstParameter) {
            return ((ConstParameter) obj).constant.equals(this.constant);
        }
        return false;
    }
}
