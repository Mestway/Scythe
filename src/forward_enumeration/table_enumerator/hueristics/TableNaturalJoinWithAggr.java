package forward_enumeration.table_enumerator.hueristics;

import com.sun.tools.javac.util.Pair;
import forward_enumeration.primitive.tables.EnumAggrTableNode;
import forward_enumeration.context.EnumContext;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/8/16.
 */
public class TableNaturalJoinWithAggr {

    public final static boolean connectAll = true;

    public static TableNode naturalTableExtensionWithAggr(EnumContext ec) {
        assert (ec.getTableNodes().size() == 1);

        TableNode tb = ec.getTableNodes().get(0);
        List<TableNode> aggrTableNodes = EnumAggrTableNode.enumAggregationNodeFlag(ec, true, true);

        TableNode currentTb = tb;

        for (TableNode agrt : aggrTableNodes) {
            int aggrFieldLength = ((AggregationNode)agrt).getAggrFieldSize();

            int tbLength = currentTb.getSchema().size();
            List<Pair<Integer, Integer>> naturalJoinPairs = new ArrayList<>();
            for (int i = 0; i < aggrFieldLength; i ++) {
                String s = agrt.getSchema().get(i);
                naturalJoinPairs.add(new Pair<>(tb.getSchema().indexOf(s), tbLength + i));
            }

            TableNode jntb = RenameTNWrapper.tryRename(new JoinNode(Arrays.asList(currentTb, agrt)));

            List<Filter> filters = new ArrayList<>();
            for (Pair<Integer, Integer> p : naturalJoinPairs) {
                filters.add(
                        new VVComparator(
                                Arrays.asList(
                                        new NamedVal(jntb.getSchema().get(p.fst)),
                                        new NamedVal(jntb.getSchema().get(p.snd))),
                                VVComparator.eq));
            }

            List nodesToSelect = jntb.getSchema()
                    .stream().map(NamedVal::new).collect(Collectors.toList()).subList(0, tbLength);

            for (int i = 0; i < agrt.getSchema().size() -  aggrFieldLength; i ++) {
                nodesToSelect.add(
                        new NamedVal(jntb.getSchema().get(tbLength + aggrFieldLength + i)));
            }

            TableNode tn = new SelectNode(
                    nodesToSelect,
                    jntb,
                    LogicAndFilter.connectByAnd(filters)
            );

            currentTb = tn;
        }

        //System.out.println(currentTb.prettyPrint(0));
        return currentTb;

    }

    // enumerate aggregation nodes of a table
    public static List<TableNode> naturalJoinWithAggregation(EnumContext ec) {

        List<TableNode> result = new ArrayList<>();

        List<TableNode> namedTables = ec.getTableNodes()
                .stream().filter(t -> (t instanceof NamedTable)).collect(Collectors.toList());

        // for each named table in the list
        for (TableNode tb : namedTables) {
            List<TableNode> aggrTableNodes = EnumAggrTableNode.enumAggregationNodeFlag(ec, true, true);

            for (TableNode agrt : aggrTableNodes) {
                // the length of fields
                int aggrFieldLength = ((AggregationNode)agrt).getAggrFieldSize();

                int tbLength = tb.getSchema().size();
                List<Pair<Integer, Integer>> naturalJoinPairs = new ArrayList<>();
                for (int i = 0; i < aggrFieldLength; i ++) {
                    String s = agrt.getSchema().get(i);
                    naturalJoinPairs.add(new Pair<>(tb.getSchema().indexOf(s), tbLength + i));
                }

                TableNode jntb = RenameTNWrapper.tryRename(new JoinNode(Arrays.asList(tb, agrt)));

                List<Filter> filters = new ArrayList<>();
                for (Pair<Integer, Integer> p : naturalJoinPairs) {
                    filters.add(
                        new VVComparator(
                            Arrays.asList(
                                new NamedVal(jntb.getSchema().get(p.fst)),
                                new NamedVal(jntb.getSchema().get(p.snd))),
                            VVComparator.eq));
                }

                List nodesToSelect = jntb.getSchema()
                        .stream().map(s -> new NamedVal(s)).collect(Collectors.toList()).subList(0, tbLength);

                for (int i = 0; i < agrt.getSchema().size() -  aggrFieldLength; i ++) {
                    nodesToSelect.add(
                            new NamedVal(jntb.getSchema().get(tbLength + aggrFieldLength + i)));
                }

                TableNode tn = new SelectNode(
                        nodesToSelect,
                        jntb,
                        LogicAndFilter.connectByAnd(filters)
                );
                result.add(tn);
            }
        }

        return result;
    }
}
