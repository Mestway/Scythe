package enumerator.tableenumerator;

import enumerator.*;
import sql.lang.Table;
import sql.lang.ast.table.TableNode;
import util.RenameTNWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/24/16.
 */
public class CanonicalWithoutExistsEnumerator extends AbstractTableEnumerator {
    @Override
    public List<TableNode> enumTable(EnumContext ec, int depth) {
        enumTableWithoutProjStrategy2(ec, depth); // ec will memoize these intermediate results, since the result pool is shared
        List<TableNode> tns = EnumProjection.enumProjection(ec, ec.getOutputTable());
        ec = EnumContext.extendTable(ec, tns);
        return ec.getTableNodes();
    }

    public static List<TableNode> enumTableWithoutProj(EnumContext ec, int depth) {

        List<TableNode> tns = new ArrayList<>();

        tns.addAll(EnumFilterNamed.enumFilterNamed(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        ec = EnumContext.extendTable(ec, tns);

        System.out.println("after enumFilterNamed: " + ec.getTableNodes().size()+ " tables");

        tns = new ArrayList<>();
        tns.addAll(EnumAggrTableNode.enumAggregationNode(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        ec = EnumContext.extendTable(ec, tns);

        System.out.println("after enumAggregationNode: " + ec.getTableNodes().size() + " tables");
        for (Table t: ec.getMemoizedTables().keySet())
            System.out.println(t);

        for (int i = 1; i <= depth; i ++) {
            tns = new ArrayList<>();
            tns.addAll(EnumJoinTableNodes.enumJoinNode(ec));
            System.out.println("[Level] " + i);
            System.out.println("There are " + tns.size() + " queries in the enumeration of this level");
            ec = EnumContext.extendTable(ec, tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
            System.out.println("after enumJoinNode: " + ec.getMemoizedTables().keySet().size() + " tables");


            Path file = Paths.get("log" + i);
            try {
                List<String> lines = new ArrayList<>();
                for (Table t : ec.getMemoizedTables().keySet()) {
                    lines.add(t.toString());
                    lines.add(" ########################################### ");
                }
                Files.write(file, lines, Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return ec.getTableNodes();
    }

    public static List<TableNode> enumTableWithoutProjStrategy2(EnumContext ec, int depth) {

        List<TableNode> tns = new ArrayList<>();

        tns.addAll(EnumFilterNamed.enumFilterNamed(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        ec = EnumContext.extendTable(ec, tns);

        System.out.println("after enumFilterNamed: " + ec.getTableNodes().size()+ " tables");

        tns = new ArrayList<>();
        tns.addAll(EnumAggrTableNode.enumAggregationNode(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
        ec = EnumContext.extendTable(ec, tns);

        System.out.println("after enumAggregationNode: " + ec.getTableNodes().size() + " tables");

        for (int i = 1; i <= depth; i ++) {
            System.out.println("[Level] " + i);

            tns = new ArrayList<>();
            tns.addAll(EnumJoinTableNodes.enumJoinNode(ec));
            System.out.println("There are " + tns.size() + " queries in the enumeration of this level");
            if (i != depth)
                ec = EnumContext.extendTable(ec,
                    tns.parallelStream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
            else
                ec = EnumContext.extendTable(ec,
                        tns.parallelStream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

            System.out.println("after enumJoinNode: " + ec.getMemoizedTables().keySet().size() + " tables");
        }

        return ec.getTableNodes();
    }
}
