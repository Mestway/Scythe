package sql.lang.ast.table;

import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;

/**
 * Created by clwang on 12/16/15.
 */
public class NamedTable implements TableNode {

    Table table;

    public NamedTable(Table table) {
        this.table = table;
    }

    @Override
    public Table eval(Environment env) {
        return table;
    }
}
