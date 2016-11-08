package forward_enumeration.table_enumerator;

import forward_enumeration.canonical_enum.components.*;
import forward_enumeration.context.EnumContext;
import forward_enumeration.container.QueryContainer;
import forward_enumeration.canonical_enum.datastructure.TableTreeNode;
import global.GlobalConfig;
import sql.lang.Table;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/31/16.
 * Perform enumeration based on SynthSQL grammar, and enumeration results are stored into query chests on the fly.
 */
public class CanonicalTableEnumeratorOnTheFly extends AbstractTableEnumerator {

    @Override
    public List<TableNode> enumTable(EnumContext ec, int depth) {

        List<TableNode> result = new ArrayList<>();

        QueryContainer qc = QueryContainer.initWithInputTables(ec.getInputs(), QueryContainer.ContainerType.TableLinks);

        enumTableWithoutProj(ec, qc, depth); // all intermediate result are stored in qc

        System.out.println("[Total Number of Intermediate Result] " + qc.getRepresentativeTableNodes().size() );
        double tableSizeSum = 0;
        for (TableNode tn : qc.getRepresentativeTableNodes()) {
            tableSizeSum += ((NamedTable)tn).getTable().getContent().size()
                    *  ((NamedTable)tn).getTable().getContent().get(0).getValues().size();
        }
        System.out.println("[Average Size of the tables] " + tableSizeSum / (qc.getRepresentativeTableNodes().size()));
        System.out.println("[Sum Size of the tables] " + tableSizeSum);

        EnumProjection.emitEnumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable(), qc);

        // this is not always enabled, as this export algorithm is only designed for canonical SQL enumerator
        if (qc.getContainerType() == QueryContainer.ContainerType.TableLinks) {

            Set<Table> leafNodes = new HashSet<>();
            leafNodes.addAll(ec.getInputs());
            List<TableTreeNode> trees = qc.getTableLinks().findTableTrees(ec.getOutputTable(), leafNodes, 4);

            int totalQueryCount = 0;
            for (TableTreeNode t : trees) {
                t.inferQuery(ec);
                result.addAll(t.treeToQuery());

                int count = t.countQueryNum();
                //System.out.println("Queries corresponds to this tree: " + count);
                totalQueryCount += count;
            }

            System.out.println("Total Tree Count: " + trees.size());
            System.out.println("Total Query Count: " + totalQueryCount);
        }

        result.sort((x,y)->Double.compare(x.estimateTotalScore(ec.getUserProvidedConstValues()), y.estimateTotalScore(ec.getUserProvidedConstValues())));

        return result;
    }

    public static void enumTableWithoutProj(EnumContext ec, QueryContainer qc, int depth) {

        //##### Synthesize natural join, for
        if (GlobalConfig.TRY_NATURAL_JOIN && false) {
            List<List<TableNode>> filterNamed = new ArrayList<>();
            for (Table t : ec.getInputs()) {
                ec.setTableNodes(Arrays.asList(new NamedTable(t)));
                filterNamed.add(EnumFilterNamed
                        .emitEnumFilterNamed(ec, qc, true)
                        .stream().map(x -> new NamedTable(x)).collect(Collectors.toList()));
            }
            List<TableNode> left = filterNamed.get(0);
            for (int i = 1; i < ec.getInputs().size(); i++) {
                left = EnumJoinTableNodes.generalEmitEnumJoin(left, filterNamed.get(i), ec, qc).stream()
                        .map(t -> new NamedTable(t)).collect(Collectors.toList());
            }
            if (EnumProjection.enumProjection(left, ec.getOutputTable()).size() > 0) return;
        }

        //##### Synthesize SPJ query
        ec.setTableNodes(ec.getInputs().stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        List<TableNode> basic = EnumFilterNamed.emitEnumFilterNamed(ec, qc, true)
                .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());

        System.out.println("[Stage 1] EnumFilterNamed: \n\t"
                + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                + "Avg table size: " + qc.getMemoizedTables().stream().map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());

        //##### Synthesize AGGR
        ec.setTableNodes(basic);
        List<TableNode> aggr = EnumAggrTableNode.emitEnumAggrNodeWFilter(ec, qc)
                .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());

        List<TableNode> basicAndAggr = new ArrayList<>();
        basicAndAggr.addAll(basic);
        basicAndAggr.addAll(aggr);

        System.out.println("[Stage 2] EnumAggregationNode: \n\t"
                + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                + "Avg table size: " + qc.getMemoizedTables().stream().map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());

        if (depth == 0) return;

        //##### Synthesize Join
        List<TableNode> leftSet = basicAndAggr;
        for (int i = 1; i <= depth; i ++) {
            //System.out.println("[Level] " + i);

            // check whether a result can be obtained by projection
            //List<TableNode> tns = EnumProjection.enumProjection(leftSet, ec.getOutputTable());
            //if (i >= 2 && ! tns.isEmpty())
             //   break;

            leftSet = EnumJoinTableNodes.generalEmitEnumJoin(leftSet, basic, ec, qc)
                    .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoinOnAggrAndBasic" + i + " \n\t"
                   // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total table by now: " + qc.getMemoizedTables().size() + "\n\t"
                    + "Avg table size: " + qc.getMemoizedTables().stream().map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());
        }
        if (! leftSet.equals(basicAndAggr))
            if (EnumProjection.enumProjection(leftSet, ec.getOutputTable()).size() > 0)
                return;

        //if (1 == 1)
        //    return;

        //##### Synthesize Union
        leftSet = basic;
        for (int i = 1; i <= depth-1; i ++) {

            // check whether a result can be obtained by projection, return if so
            // if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;

            leftSet = EnumUnion.emitEnumerateUnion(leftSet, basic, qc)
                    .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumLeftJoin" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
        }

        if (!leftSet.equals(basic)) {
            if (EnumProjection.enumProjection(leftSet, ec.getOutputTable()).size() > 0)
                return;
        }

        //##### Synthesize LeftJoin
        leftSet = basic;
        for (int i = 1; i <= depth-1; i ++) {

            // check whether a result can be obtained by projection
            // if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;

            leftSet = EnumLeftJoin.emitEnumLeftJoin(leftSet, basic, qc)
                        .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumUnion" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
        }

        if (! leftSet.equals(basic)) {
            if (EnumProjection.enumProjection(leftSet, ec.getOutputTable()).size() > 0)
                return;
        }

        //##### Synthesize Aggregation on Join
        leftSet = basic;
        for (int i = 1; i <= depth-1; i ++) {
            // check whether a result can be obtained by projection
            //if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;
            leftSet = EnumJoinTableNodes.generalEmitEnumJoin(leftSet, basic, ec, qc)
                    .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoinOnAggrAndBasic" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                    + "Avg table size: " + qc.getMemoizedTables()
                    .stream()
                    .map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());
        }
        if (leftSet != basic) {
            ec.setTableNodes(leftSet);
            List<TableNode> tmp = EnumAggrTableNode.emitEnumAggrNodeWFilter(ec, qc).stream()
                    .map(t -> new NamedTable(t)).collect(Collectors.toList());
            if (EnumProjection.enumProjection(tmp, ec.getOutputTable()).size() > 0) return;
        }


        //##### Synthesize join with aggregation result
        leftSet = basicAndAggr;
        for (int i = 1; i <= depth; i ++) {
            //System.out.println("[Level] " + i);

            // check whether a result can be obtained by projection
            // if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;

            leftSet = EnumJoinTableNodes.generalEmitEnumJoin(leftSet, basicAndAggr, ec, qc)
                    .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoinTwoAggr" + i + " \n\t"
                    // + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                    + "Avg table size: " + qc.getMemoizedTables().stream().map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());
        }
        if (!leftSet.equals(basicAndAggr)) {
            if (EnumProjection.enumProjection(leftSet, ec.getOutputTable()).size() > 0)
                return;
        }

        //##### Synthesize Aggr after Aggr
        ec.setTableNodes(aggr);
        List<TableNode> aggrAfterAggr = EnumAggrTableNode.emitEnumAggrNodeWFilter(ec, qc)
                .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());
        leftSet = aggrAfterAggr;
        for (int i = 1; i <= depth-1; i ++) {
            // check whether a result can be obtained by projection
            //if (EnumProjection.enumProjection(qc.getRepresentativeTableNodes(), ec.getOutputTable()).size() > 0) return;

            leftSet = EnumJoinTableNodes.generalEmitEnumJoin(leftSet, basic, ec, qc)
                    .stream().map(t -> new NamedTable(t)).collect(Collectors.toList());
            System.out.println("[Stage " + (2 + i) + "] EnumJoinOnAggrAggr" + i + " \n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size() + "\n\t"
                    + qc.getMemoizedTables().stream()
                    .map(t -> t.getContent().size() * t.getSchema().size()).reduce((x,y)-> x + y).get() / qc.getMemoizedTables().size());
        }
    }
}
