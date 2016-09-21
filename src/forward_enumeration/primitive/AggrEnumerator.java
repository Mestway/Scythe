package forward_enumeration.primitive;

import forward_enumeration.context.EnumContext;
import forward_enumeration.context.QueryChest;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.RenameTableNode;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import util.CombinationGenerator;
import util.Pair;
import util.RenameTNWrapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 9/20/16.
 * An utility funciton for enumeration of aggregation table nodes.
 */
public class AggrEnumerator {
    /**
     * Filters are not considered here, enumerating aggregation with filters require a stand alone pipeline.
     * @param tn the table to perform aggregation on
     * @return the list of enumerated table based on the given tablenode
     */
    private static List<RenameTableNode> enumerateAggregation(
            EnumContext ec,
            TableNode tn,
            boolean simplify) {

        List<RenameTableNode> aggrNodes = new ArrayList<>();

        List<List<String>> groupByFieldsComb = CombinationGenerator.genCombination(tn.getSchema());
        groupByFieldsComb.add(new ArrayList<>());

        for (List<String> groupByFields : groupByFieldsComb) {

            // Not sure if this is correct or not
            // TODO: make sure this works in the future, we don't want to group on something that has no effect
            try {
                Table table = tn.eval(new Environment());
                if (AggregationNode.numberOfGroups(table, groupByFields) == table.getContent().size())
                    continue;
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }

            // Part I: add the group by query without aggregation into the group by fields
            if (! groupByFields.isEmpty()) {
                RenameTableNode query = (RenameTableNode) RenameTNWrapper
                        .tryRename(new AggregationNode(tn,
                                groupByFields, new ArrayList<Pair<String, Function<List<Value>, Value>>>()));

                aggrNodes.add(query);
            }

            // If the group by size is equal to the schema size, we shall skip the process of finding a target field
            // if (groupByFields.size() == tn.getSchema().size())
            // continue;

            // Part II: find queries with a target field.
            List<Pair<String, Function<List<Value>, Value>>> targetFuncList = new ArrayList<>();

            // Then enum the target fields
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                String targetField = tn.getSchema().get(i);
                ValType targetType = tn.getSchemaType().get(i);

                // in this case, aggregation will not provide any useful information
                if (groupByFields.contains(targetField) && groupByFields.size() == 1)
                    continue;

                // Find the target and the aggregation fields now, start generation
                List<Function<List<Value>, Value>> aggrFuncs = ec.getAggrFuns(targetType);

                // Last step, enumerate all group-by functions
                for (Function<List<Value>, Value> f : aggrFuncs) {
                    targetFuncList.add(new Pair<>(targetField, f));
                }
            }

            if (! simplify) {
                // allow comparison between different rows, since only one table is added to the result
                // the table with allowing multiple table
                aggrNodes.add((RenameTableNode) RenameTNWrapper.tryRename(new AggregationNode(tn, groupByFields, targetFuncList)));
            } else {
                // each aggregation function will create only one field
                for (Pair<String, Function<List<Value>, Value>> p : targetFuncList) {
                    // we perform a renaming to ensure consistency for other parts
                    RenameTableNode rt = (RenameTableNode) RenameTNWrapper
                            .tryRename(new AggregationNode(tn, groupByFields, Arrays.asList(p)));

                    // add the aggregation query without filter in to the result
                    aggrNodes.add(rt);
                }
            }
        }
        return aggrNodes;
    }
}
