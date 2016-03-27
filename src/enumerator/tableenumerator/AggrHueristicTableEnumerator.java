package enumerator.tableenumerator;

import enumerator.context.EnumContext;
import enumerator.EnumSelTableNode;
import enumerator.hueristics.TableNaturalJoinWithAggr;
import sql.lang.ast.table.TableNode;
import util.RenameTNWrapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This enumerator features precomputing aggregation before enumerating other things
 * Created by clwang on 3/21/16.
 */
public class AggrHueristicTableEnumerator extends  AbstractTableEnumerator {
    @Override
    public List<TableNode> enumTable(EnumContext ec, int depth) {
        List<TableNode> agrTables = TableNaturalJoinWithAggr.naturalJoinWithAggregation(ec);
        ec = EnumContext.extendTable(ec,
                agrTables.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        for (int i = 0; i < depth; i ++) {
            List<TableNode> tableNodes = EnumSelTableNode.enumSelectNode(ec);
            ec = EnumContext.extendTable(ec, tableNodes);
            /*tableNodes = EnumJoinTableNodes.enumJoinNode(ec);
            ec = EnumContext.extendTable(ec,
                    tableNodes.stream()
                            .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList())); */
        }
        return ec.getTableNodes();
    }
}
