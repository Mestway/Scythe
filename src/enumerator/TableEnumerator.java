package enumerator;

import enumerator.hueristics.TableNaturalJoinWithAggr;
import enumerator.parameterized.EnumParamTN;
import jdk.nashorn.internal.runtime.Debug;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.DebugHelper;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/7/16.
 */
public class TableEnumerator {

    public static boolean enumWithPrecomputedAggregation = false;

    public static List<TableNode> enumProgramWithIO(List<Table> input, Table output, Constraint c) {

        List<ValNode> vns = new ArrayList<>();
        vns.addAll(c.constValNodes());

        List<TableNode> parameterizedTables = EnumParamTN
                .enumParameterizedTableNodes(
                        input.stream()
                                .map(t -> new NamedTable(t)).collect(Collectors.toList()),
                        vns,
                        c.getNumberOfParam());

        System.out.println("There are " + parameterizedTables.size() + " tables involved in this enumeration.");

        EnumContext ec = new EnumContext(input, c);
        ec.setParameterizedTables(parameterizedTables);

        List<TableNode> tns = new ArrayList<>();

        if (enumWithPrecomputedAggregation)
            tns = enumTableWithPrecomputedAggregation(ec, c.maxDepth);
        else
            enumTable(ec, c.maxDepth); // enumeration results already stored in EC

        System.out.println("I reach this place!");

        tns = EnumProjection.enumProjection(ec, output);
        ec = EnumContext.extendTable(ec, tns);

        System.out.println("I've done projection!");

        // only keep the table nodes that are consistent with the I/O pairs
        List<TableNode> valid = tns.stream().filter(tn -> {
            try {
                return tn.eval(new Environment()).contentStrictEquals(output);
            } catch (SQLEvalException e) {
                return false;
            }
        }).collect(Collectors.toList());

        System.out.println("There are " + valid.size() + " tables in the world.");

        if (valid.isEmpty())
            return valid;
        else {
            List<TableNode> tss = ec.lookup(output);
            List<TableNode> result = new ArrayList<>();
            for (TableNode ts : tss) {
                result.addAll(ec.export(ts, new ArrayList<>(),
                        input.stream().map(i -> new NamedTable(i)).collect(Collectors.toList())));
            }
            return result;
        }
    }

    public static List<TableNode> enumTable(EnumContext ec, int depth) {

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

    public static List<TableNode> enumTableWithPrecomputedAggregation(EnumContext ec, int depth) {

        // enumerate depth 0 tables
        List<TableNode> tbs = ec.getTableNodes();
        tbs.addAll(TableNaturalJoinWithAggr.naturalJoinWithAggregation(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        List<TableNode> aggrNodes = EnumAggrTableNode.enumAggregationNode(ec);
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
