package enumerator;

import enumerator.hueristics.TableNaturalJoinWithAggr;
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
 * Created by clwang on 1/7/16.
 */
public class TableEnumerator {

    public static List<TableNode> enumProgramWithIO(List<Table> input, Table output, Constraint c) {

        List<ValNode> vns = new ArrayList<>();
        vns.addAll(c.constValNodes);

        List<TableNode> parameterizedTables = EnumParamTN
                .enumParameterizedTableNodes(
                        input.stream()
                                .map(t -> new NamedTable(t)).collect(Collectors.toList()),
                        vns,
                        c.getNumberOfParam());

        EnumContext ec = new EnumContext(input, c);
        ec.setParameterizedTables(parameterizedTables);

        List<TableNode> tns = enumTableWithPrecomputedAggregation(ec, c.maxDepth);

        // only keep the table nodes that are consistent with the I/O pairs
        List<TableNode> valid = tns.stream().filter(tn -> {
            try {
                return tn.eval(new Environment()).contentEquals(output);
            } catch (SQLEvalException e) {
                return false;
            }
        }).collect(Collectors.toList());

        if (EnumContext.noMemoization)
            return valid;

        if (valid.isEmpty())
            return valid;
        else {
            return ec.lookup(output);
        }
    }

    public static List<TableNode> enumTable(EnumContext ec, int depth) {

        List<TableNode> tns = new ArrayList<>();
        tns.addAll(enumTable(ec, depth - 1));

        if (depth == 1)
            tns.addAll(EnumAggrTableNode.enumAggregationNode(ec, depth));

        for (int i = 1; i <= depth; i ++) {
            tns.addAll(EnumSelTableNode.enumSelectNode(ec));
            tns.addAll(EnumJoinTableNodes.enumJoinNode(ec));
            ec = EnumContext.extendTable(ec, tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        }

        return ec.getTableNodes();
    }

    public static List<TableNode> enumTableWithPrecomputedAggregation(EnumContext ec, int depth) {

        // enumerate depth 0 tables
        List<TableNode> tbs = ec.getTableNodes();
        tbs.addAll(TableNaturalJoinWithAggr.naturalJoinWithAggregation(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        List<TableNode> aggrNodes = EnumAggrTableNode.enumAggregationNode(ec, 0);
        tbs.addAll(aggrNodes.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        ec = EnumContext.extendTable(ec, tbs);

        for (int i  = 1; i <= depth; i ++) {
            List<TableNode> tns = new ArrayList<>();
            tns.addAll(EnumSelTableNode.enumSelectNode(ec));
            tns.addAll(EnumJoinTableNodes.enumJoinNode(ec));
            List<TableNode> renamed = tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
            ec = EnumContext.extendTable(ec, renamed);
        }

        return ec.getTableNodes();
    }
}
