package enumerator.tableenumerator;

import enumerator.*;
import enumerator.parameterized.EnumParamTN;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public class CanonicalTableEnumerator extends AbstractTableEnumerator {

    @Override
    public List<TableNode> enumTable(EnumContext ec, int depth) {
        enumTableWithoutProj(ec, depth); // ec will memoize these intermediate results, since the result pool is shared
        List<TableNode> tns = EnumProjection.enumProjection(ec, ec.getOutputTable());
        ec = EnumContext.extendTable(ec, tns);
        return ec.getTableNodes();
    }

    public static List<TableNode> enumTableWithoutProj(EnumContext ec, int depth) {

        List<TableNode> tns = new ArrayList<>();

        tns.addAll(EnumFilterNamed.enumFilterNamed(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        ec = EnumContext.extendTable(ec, tns);

        tns = new ArrayList<>();
        tns.addAll(EnumAggrTableNode.enumAggregationNode(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        ec = EnumContext.extendTable(ec, tns);

        for (int i = 1; i <= depth; i ++) {
            tns = new ArrayList<>();
            tns.addAll(EnumJoinTableNodes.enumJoinNode(ec));
            System.out.println("There are " + tns.size() + " tables in the enumeration of this level(" + i + ")");
            ec = EnumContext.extendTable(ec, tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        }

        return ec.getTableNodes();
    }
}
