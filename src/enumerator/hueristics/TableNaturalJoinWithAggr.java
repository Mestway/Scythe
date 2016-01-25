package enumerator.hueristics;

import com.sun.tools.javac.util.Pair;
import enumerator.EnumAggrTableNode;
import enumerator.EnumContext;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.JoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
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

    // enumerate aggregation nodes of a table
    public static List<TableNode> naturalJoinWithAggregation(EnumContext ec) {

        List<TableNode> result = new ArrayList<>();

        List<TableNode> namedTables = ec.getTableNodes()
                .stream().filter(t -> (t instanceof NamedTable)).collect(Collectors.toList());

        // for each named table in the list
        for (TableNode tb : namedTables) {
            List<TableNode> aggrTableNodes = EnumAggrTableNode.enumAggregationNode(ec, 1);
            for (TableNode agrt : aggrTableNodes) {
                int tbLength = tb.getSchema().size();
                List<Pair<Integer, Integer>> naturalJoinPairs = new ArrayList<>();
                int i = 0;
                for (String s : agrt.getSchema().subList(0, agrt.getSchema().size()-1)) {
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
                nodesToSelect.add(new NamedVal(jntb.getSchema().get(jntb.getSchema().size()-1)));

                result.add(
                    new SelectNode(
                        nodesToSelect,
                        jntb,
                        LogicAndFilter.ConnectByAnd(filters)
                    )
                );
            }
        }

        return result;
    }
}
