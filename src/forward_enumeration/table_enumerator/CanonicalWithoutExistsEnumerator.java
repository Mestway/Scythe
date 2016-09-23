package forward_enumeration.table_enumerator;

import forward_enumeration.context.EnumContext;
import forward_enumeration.container.QueryContainer;
import forward_enumeration.enumerative_search.components.EnumAggrTableNode;
import forward_enumeration.enumerative_search.components.EnumFilterNamed;
import forward_enumeration.enumerative_search.components.EnumJoinTableNodes;
import forward_enumeration.enumerative_search.components.EnumProjection;
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
        QueryContainer qc = QueryContainer.initWithInputTables(ec.getInputs());
        qc = enumTableWithoutProjStrategy2(ec, qc, depth); // all intermediate result in qc is stored

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumProjection.enumProjection(ec, ec.getOutputTable());
        qc.insertQueries(tns);
        return tns;
    }

    public static QueryContainer enumTableWithoutProj(EnumContext ec, QueryContainer qc, int depth) {

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumFilterNamed.enumFilterNamed(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
        qc.insertQueries(tns);

        System.out.println("After enumFilterNamed: " + qc.getRepresentativeTableNodes().size()+ " tables");

        // prepare ec for next enumeration iteration
        ec.setTableNodes(qc.getRepresentativeTableNodes());
        tns = EnumAggrTableNode.enumAggrNodeWFilter(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());

        qc.insertQueries(tns);

        System.out.println("After enumAggrNodeWFilter: " + ec.getTableNodes().size() + " tables");
        for (Table t: qc.getMemoizedTables())
            System.out.println(t);

        for (int i = 1; i <= depth; i ++) {
            tns = new ArrayList<>();

            // enumerate join
            ec.setTableNodes(qc.getRepresentativeTableNodes());
            tns.addAll(EnumJoinTableNodes.enumJoinWithFilter(ec));

            System.out.println("[Level] " + i);
            System.out.println("There are " + tns.size() + " queries in the enumeration of this level");
            qc.insertQueries(tns.stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));
            System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");


            Path file = Paths.get("log" + i);
            try {
                List<String> lines = new ArrayList<>();
                for (Table t : qc.getMemoizedTables()) {
                    lines.add(t.toString());
                    lines.add(" ########################################### ");
                }
                Files.write(file, lines, Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return qc;
    }

    public static QueryContainer enumTableWithoutProjStrategy2(EnumContext ec, QueryContainer qc, int depth) {

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumFilterNamed.enumFilterNamed(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
        qc.insertQueries(tns);

        System.out.println("after enumFilterNamed: " + qc.getRepresentativeTableNodes().size()+ " tables");

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        tns = EnumAggrTableNode.enumAggrNodeWFilter(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
        qc.insertQueries(tns);

        System.out.println("after enumAggrNodeWFilter: " + qc.getRepresentativeTableNodes().size() + " tables");

        for (int i = 1; i <= depth; i ++) {
            System.out.println("[Level] " + i);

            tns = new ArrayList<>();
            ec.setTableNodes(qc.getRepresentativeTableNodes());

            tns.addAll(EnumJoinTableNodes.enumJoinWithFilter(ec));
            System.out.println("There are " + tns.size() + " queries in the enumeration of this level");

            List<TableNode> renamed = tns.parallelStream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());

            System.out.println("After renamed: " + renamed.size());

            qc.insertQueries(renamed);

            System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
        }

        return qc;
    }
}
