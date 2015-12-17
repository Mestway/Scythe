package sql.lang.ast.val;

import sql.lang.DataType.Value;
import sql.lang.ast.Environment;

/**
 * Created by clwang on 12/16/15.
 */
public class ConstantVal implements ValNode {
    Value val;

    public void setVal(Value val) {
        this.val = val;
    }

    public Value eval(Environment env) {
        return val;
    }
}
