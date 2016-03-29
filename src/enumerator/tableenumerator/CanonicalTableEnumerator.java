package enumerator.tableenumerator;

import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import enumerator.primitive.EnumAggrTableNode;
import enumerator.primitive.EnumFilterNamed;
import enumerator.primitive.EnumJoinTableNodes;
import enumerator.primitive.EnumProjection;
import sql.lang.ast.table.TableNode;
import util.RenameTNWrapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public class CanonicalTableEnumerator extends AbstractTableEnumerator {

    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {
        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());
        enumTableWithoutProj(ec, qc, depth); // ec will memoize these intermediate results, since the result pool is shared

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumProjection.enumProjection(ec, ec.getOutputTable());
        qc.updateQueries(tns);

        return qc;
    }

    public static QueryChest enumTableWithoutProj(EnumContext ec, QueryChest qc, int depth) {

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumFilterNamed.enumFilterNamed(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
        qc.updateQueries(tns);

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        tns = EnumAggrTableNode.enumAggregationNode(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
        qc.updateQueries(tns);

        for (int i = 1; i <= depth; i ++) {
            ec.setTableNodes(qc.getRepresentativeTableNodes());
            tns = EnumJoinTableNodes.enumJoinNode(ec);
            System.out.println("There are " + tns.size() + " tables in the enumeration of this level(" + i + ")");
            qc.updateQueries(tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        }

        return qc;
    }
}
