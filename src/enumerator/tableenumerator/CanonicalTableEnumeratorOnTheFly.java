package enumerator.tableenumerator;

import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import enumerator.primitive.EnumAggrTableNode;
import enumerator.primitive.EnumFilterNamed;
import enumerator.primitive.EnumJoinTableNodes;
import enumerator.primitive.EnumProjection;
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
 * Created by clwang on 3/31/16.
 */
public class CanonicalTableEnumeratorOnTheFly extends AbstractTableEnumerator {
    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {

        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());
        qc = enumTableWithoutProjStrategy2(ec, qc, depth); // all intermediate result in qc is stored

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumProjection.enumProjection(ec, ec.getOutputTable());
        qc.updateQueries(tns);

        return qc;
    }

    public static QueryChest enumTableWithoutProjStrategy2(EnumContext ec, QueryChest qc, int depth) {

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        EnumFilterNamed.emitEnumFilterNamed(ec, qc);

        System.out.println("after enumFilterNamed: " + qc.getRepresentativeTableNodes().size()+ " tables");

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        EnumAggrTableNode.emitEnumAggregationNode(ec, qc);

        System.out.println("after enumAggregationNode: " + qc.getRepresentativeTableNodes().size() + " tables");

        for (int i = 1; i <= depth; i ++) {
            System.out.println("[Level] " + i);

            ec.setTableNodes(qc.getRepresentativeTableNodes());

            EnumJoinTableNodes.emitEnumJoinWithFilter(ec, qc);
            //System.out.println("There are " + tns.size() + " queries in the enumeration of this level");
            //List<TableNode> renamed = tns.parallelStream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
            // System.out.println("After renamed: " + renamed.size());
            //qc.updateQueries(renamed);

            System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
        }

        return qc;
    }
}
