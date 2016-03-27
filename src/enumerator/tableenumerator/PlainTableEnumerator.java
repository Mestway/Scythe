package enumerator.tableenumerator;

import enumerator.EnumAggrTableNode;
import enumerator.context.EnumContext;
import enumerator.EnumJoinTableNodes;
import enumerator.EnumSelTableNode;
import sql.lang.ast.table.TableNode;
import util.RenameTNWrapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public class PlainTableEnumerator extends AbstractTableEnumerator {
    @Override
    public List<TableNode> enumTable(EnumContext ec, int depth) {
        List<TableNode> agrTables = EnumAggrTableNode.enumAggregationNode(ec);
        ec = EnumContext.extendTable(ec,
                agrTables.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        for (int i = 0; i < depth; i ++) {
            List<TableNode> tableNodes = EnumJoinTableNodes.enumJoinWithoutFilter(ec);
            ec = EnumContext.extendTable(ec,
                    tableNodes.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
            tableNodes = EnumSelTableNode.enumSelectNode(ec);
            ec = EnumContext.extendTable(ec, tableNodes);
        }

        return ec.getTableNodes();
    }
}
