package enumerator;

import enumerator.hueristics.NaturalTableExtension;
import enumerator.parameterized.EnumParamTN;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;
import util.DebugHelper;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/7/16.
 */
public class TableEnumerator {

    public static List<TableNode> enumProgramWithIO(List<Table> input, Table output, Constraint c) {
        /*List<TableNode> parameterizedTables = EnumParamTN
                .enumParameterizedTableNodes(
                        input.stream()
                                .map(t -> new NamedTable(t)).collect(Collectors.toList()),
                        new ArrayList<>().addAll(c.constValNodes),
                        2);
        */
        EnumContext ec = new EnumContext(input, c);
        //ec.setParameterizedTables(parameterizedTables);

        List<TableNode> tns = enumTableWithHueristics(ec, c.maxDepth);

        DebugHelper.printTableNodes(tns);

        // only keep the table nodes that are consistent with the I/O pairs
        return tns.stream().filter(tn -> {
            try {
                return tn.eval(new Environment()).contentEquals(output);
            } catch (SQLEvalException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    static Map<Integer, List<TableNode>> map = new HashMap<>();

    public static List<TableNode> enumTable(EnumContext ec, int depth) {

        if (map.containsKey(depth))
            return map.get(depth);

        if (depth == 0) {
            List<TableNode> tbs = ec.getTableNodes();
            map.put(0, tbs);
            return map.get(0);
        }

        List<TableNode> tns = new ArrayList<>();
        tns.addAll(enumTable(ec, depth - 1));

        if (depth == 1)
            tns.addAll(EnumAggrTableNode.enumAggregationNode(ec, depth));

        tns.addAll(EnumSelTableNode.enumSelectNode(ec, depth));
        tns.addAll(EnumJoinTableNodes.enumJoinNode(ec, depth));
        map.put(depth, tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        return map.get(depth);
    }

    public static List<TableNode> enumTableWithHueristics(EnumContext ec, int depth) {

        if (map.containsKey(depth))
            return map.get(depth);

        if (depth == 0) {
            List<TableNode> tbs = ec.getTableNodes();
            tbs.addAll(NaturalTableExtension.naturalJoinWithAggregation(ec)
                    .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

            List<TableNode> aggrNodes = EnumAggrTableNode.enumAggregationNode(ec, 0);
            tbs.addAll(aggrNodes.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
            DebugHelper.printTableNodes(tbs);
            map.put(0, tbs);
            return map.get(0);
        }

        List<TableNode> tns = new ArrayList<>();
        tns.addAll(enumTableWithHueristics(ec, depth - 1));

        tns.addAll(EnumSelTableNode.enumSelectNode(ec, depth));
        tns.addAll(EnumJoinTableNodes.enumJoinNode(ec, depth));
        map.put(depth, tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        return map.get(depth);
    }
}
