package forward_enumeration.table_enumerator;

import forward_enumeration.enumerative_search.components.EnumAggrTableNode;
import forward_enumeration.context.EnumContext;
import forward_enumeration.enumerative_search.components.EnumJoinTableNodes;
import forward_enumeration.primitive.parameterized.EnumSelectTableNode;
import forward_enumeration.container.QueryContainer;
import sql.lang.ast.table.TableNode;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public class PlainTableEnumerator extends AbstractTableEnumerator {
    @Override
    public List<TableNode> enumTable(EnumContext ec, int depth) {

        List<TableNode> result = new ArrayList<>();

        QueryContainer qc = QueryContainer.initWithInputTables(ec.getInputs());
        List<TableNode> agrTables = EnumAggrTableNode.enumAggrNodeWFilter(ec);
        qc.insertQueries(agrTables.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        for (int i = 0; i < depth; i ++) {
            ec.setTableNodes(qc.getRepresentativeTableNodes());
            List<TableNode> tableNodes = EnumJoinTableNodes.enumJoinWithoutFilter(ec).stream()
                    .map(jn -> (TableNode) jn).collect(Collectors.toList());
            qc.insertQueries(tableNodes.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
            tableNodes = EnumSelectTableNode.enumSelectNode(ec);
            qc.insertQueries(tableNodes);
            result.addAll(tableNodes);
        }

        return result;
    }
}
