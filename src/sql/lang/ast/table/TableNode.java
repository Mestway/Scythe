package sql.lang.ast.table;

import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Node;

/**
 * Created by clwang on 12/16/15.
 */
public interface TableNode extends Node {
    Table eval(Environment env);
}
