package sql.lang.ast.val;

import com.sun.tools.doclint.Env;
import sql.lang.DataType.Value;
import sql.lang.ast.Environment;
import sql.lang.ast.Node;

/**
 * Created by clwang on 12/16/15.
 */
public interface ValNode extends Node {
    public Value eval(Environment env);
}
