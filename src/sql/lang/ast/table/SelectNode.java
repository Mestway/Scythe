package sql.lang.ast.table;

import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.val.ValNode;
import sql.lang.ast.filter.Filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by clwang on 12/16/15.
 */
public class SelectNode implements TableNode {

    TableNode tableNode;
    Filter filter;

    public SelectNode(TableNode node, Filter filter) {
        this.tableNode = node;
        this.filter = filter;
    }

    @Override
    public Table eval(Environment env) {

        Table table = tableNode.eval(env);

        Table resultTable = table.duplicate();
        resultTable.getContent().clear();
        resultTable.setName(Table.AssignNewName());

        for (TableRow row : table.getContent()) {
            // The name and value binding between field names and values
            Map<String, Value> rowBinding = new HashMap<String, Value>();

            if (table.getName().equals("anonymous")) {
                for (int i = 0; i < table.getMetadata().size(); i ++) {
                    rowBinding.put(table.getMetadata().get(i), row.getValue(i));
                }
            } else {
                for (int i = 0; i < table.getMetadata().size(); i ++) {
                    rowBinding.put(
                            table.getName() + "." + table.getMetadata().get(i),
                            row.getValue(i));
                }
            }

            Environment extEnv = env.extend(rowBinding);

            if (filter.filter(extEnv)) {
                resultTable.getContent().add(row.duplicate());
            }
        }

        return resultTable;
    }
}
