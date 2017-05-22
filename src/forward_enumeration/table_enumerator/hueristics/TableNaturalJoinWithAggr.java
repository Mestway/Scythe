package forward_enumeration.table_enumerator.hueristics;

import forward_enumeration.canonical_enum.components.EnumAggrTableNode;
import forward_enumeration.context.EnumContext;
import lang.sql.ast.predicate.Predicate;
import lang.sql.ast.predicate.LogicAndPred;
import lang.sql.ast.predicate.BinopPred;
import lang.sql.ast.contable.*;
import lang.sql.ast.valnode.NamedVal;
import util.Pair;
import util.RenameWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/8/16.
 */
public class TableNaturalJoinWithAggr {

    public static TableNode naturalTableExtensionWithAggr(EnumContext ec) {
        assert (ec.getTableNodes().size() == 1);

        TableNode tb = ec.getTableNodes().get(0);
        List<TableNode> aggrTableNodes = EnumAggrTableNode.enumAggrNodeWFilter(ec);

        TableNode currentTb = tb;

        for (TableNode agrt : aggrTableNodes) {
            int aggrFieldLength = ((AggregationNode)agrt).getAggrFieldSize();

            int tbLength = currentTb.getSchema().size();
            List<Pair<Integer, Integer>> naturalJoinPairs = new ArrayList<>();
            for (int i = 0; i < aggrFieldLength; i ++) {
                String s = agrt.getSchema().get(i);
                naturalJoinPairs.add(new Pair<>(tb.getSchema().indexOf(s), tbLength + i));
            }

            TableNode jntb = RenameWrapper.tryRename(new JoinNode(Arrays.asList(currentTb, agrt)));

            List<Predicate> filters = new ArrayList<>();
            for (Pair<Integer, Integer> p : naturalJoinPairs) {
                filters.add(
                        new BinopPred(
                                Arrays.asList(
                                        new NamedVal(jntb.getSchema().get(p.getKey())),
                                        new NamedVal(jntb.getSchema().get(p.getValue()))),
                                BinopPred.eq));
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
                    LogicAndPred.connectByAnd(filters)
            );

            currentTb = tn;
        }

        return currentTb;

    }

    // enumerate aggregation nodes of a table
    public static List<TableNode> naturalJoinWithAggregation(EnumContext ec) {

        List<TableNode> result = new ArrayList<>();

        List<TableNode> namedTables = ec.getTableNodes()
                .stream().filter(t -> (t instanceof NamedTableNode)).collect(Collectors.toList());

        // for each named table in the list
        for (TableNode tb : namedTables) {
            List<TableNode> aggrTableNodes = EnumAggrTableNode.enumAggrNodeWFilter(ec);

            for (TableNode agrt : aggrTableNodes) {
                // the length of fields
                int aggrFieldLength = ((AggregationNode)agrt).getAggrFieldSize();

                int tbLength = tb.getSchema().size();
                List<Pair<Integer, Integer>> naturalJoinPairs = new ArrayList<>();
                for (int i = 0; i < aggrFieldLength; i ++) {
                    String s = agrt.getSchema().get(i);
                    naturalJoinPairs.add(new Pair<>(tb.getSchema().indexOf(s), tbLength + i));
                }

                TableNode jntb = RenameWrapper.tryRename(new JoinNode(Arrays.asList(tb, agrt)));

                List<Predicate> filters = new ArrayList<>();
                for (Pair<Integer, Integer> p : naturalJoinPairs) {
                    filters.add(
                        new BinopPred(
                            Arrays.asList(
                                new NamedVal(jntb.getSchema().get(p.getKey())),
                                new NamedVal(jntb.getSchema().get(p.getValue()))),
                            BinopPred.eq));
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
                        LogicAndPred.connectByAnd(filters)
                );
                result.add(tn);
            }
        }

        return result;
    }
}
