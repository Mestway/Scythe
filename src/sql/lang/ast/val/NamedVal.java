package sql.lang.ast.val;

import sql.lang.DataType.Value;
import sql.lang.ast.Environment;

/**
 * Created by clwang on 12/16/15.
 */
public class NamedVal implements ValNode {

    String name;

    public NamedVal(String name) {
        this.name = name;
    }

    @Override
    public Value eval(Environment env) {
        return env.lookup(name);
    }
}
