package forward_enumeration.table_enumerator;

import forward_enumeration.context.EnumContext;
import forward_enumeration.context.QueryChest;
import forward_enumeration.primitive.tables.EnumAggrTableNode;
import forward_enumeration.primitive.tables.EnumFilterNamed;
import forward_enumeration.primitive.tables.EnumJoinTableNodes;
import forward_enumeration.primitive.tables.EnumProjection;

/**
 * Created by clwang on 3/31/16.
 */
public class CanonicalTableEnumeratorOnTheFly extends AbstractTableEnumerator {

    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {

        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());
        qc.setEnableExport();

        enumTableWithoutProj(ec, qc, depth); // all intermediate result are stored in qc

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        EnumProjection.emitEnumProjection(ec, ec.getOutputTable(), qc);

        System.out.println("[Runner up Table Count] " + qc.runnerUpTable);

        return qc;
    }

    public static void enumTableWithoutProj(EnumContext ec, QueryChest qc, int depth) {

        int lastQueryCount = 0;
        ec.setTableNodes(qc.getRepresentativeTableNodes());
        EnumFilterNamed.emitEnumFilterNamed(ec, qc);

        System.out.println("[Stage 1] EnumFilterNamed: \n\t"
                + "Queries generated: " + (qc.queryCount - lastQueryCount) + "\n\t"
                //+ "Tables generated: " + (qc.tracked.size()) + "\n\t"
                + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
        lastQueryCount = qc.queryCount;
        //qc.tracked.clear();

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        EnumAggrTableNode.emitEnumAggregationNode(ec, qc);

        System.out.println("[Stage 2] EnumAggregationNode: \n\t"
                + "Queries generated: " + (qc.queryCount - lastQueryCount) + "\n\t"
                //+ "Tables generated: " + (qc.tracked.size()) + "\n\t"
                + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
        lastQueryCount = qc.queryCount;

        // qc.tracked.clear();

        for (int i = 1; i <= depth; i ++) {
            //System.out.println("[Level] " + i);

            ec.setTableNodes(qc.getRepresentativeTableNodes());

            EnumJoinTableNodes.emitEnumJoinWithFilter(ec, qc);
            //System.out.println("There are " + tns.size() + " queries in the enumeration of this level");
            //List<TableNode> renamed = tns.parallelStream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
            // System.out.println("After renamed: " + renamed.size());
            //qc.updateQueries(renamed);

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoin" + i + " \n\t"
                    + "Queries generated: " + (qc.queryCount - lastQueryCount) + "\n\t"
                   // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
            lastQueryCount = qc.queryCount;
           // qc.tracked.clear();
        }
    }
}
