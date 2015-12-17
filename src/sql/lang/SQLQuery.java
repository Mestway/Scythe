package sql.lang;

import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;

/**
 * Created by clwang on 12/16/15.
 */
public class SQLQuery {
    TableNode queryAst;

    public SQLQuery(TableNode ast) {
        this.queryAst = ast;
    }

    public Table execute() {
        return queryAst.eval(new Environment());
    }
}
