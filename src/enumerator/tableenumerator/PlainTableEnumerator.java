package enumerator.tableenumerator;

import enumerator.primitive.tables.EnumAggrTableNode;
import enumerator.context.EnumContext;
import enumerator.primitive.tables.EnumJoinTableNodes;
import enumerator.primitive.tables.EnumSelTableNode;
import enumerator.context.QueryChest;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.RenameTNWrapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public class PlainTableEnumerator extends AbstractTableEnumerator {
    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {

        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());
        List<TableNode> agrTables = EnumAggrTableNode.enumAggregationNode(ec);
        qc.updateQueries(agrTables.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        for (int i = 0; i < depth; i ++) {
            ec.setTableNodes(qc.getRepresentativeTableNodes());
            List<TableNode> tableNodes = EnumJoinTableNodes.enumJoinWithoutFilter(ec).stream()
                    .map(jn -> (TableNode) jn).collect(Collectors.toList());
            qc.updateQueries(tableNodes.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
            tableNodes = EnumSelTableNode.enumSelectNode(ec);
            qc.updateQueries(tableNodes);
        }

        return qc;
    }
}
