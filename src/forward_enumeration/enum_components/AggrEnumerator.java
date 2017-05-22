package forward_enumeration.enum_components;

import forward_enumeration.context.EnumContext;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.contable.AggregationNode;
import lang.sql.ast.contable.RenameTableNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import lang.sql.exception.SQLEvalException;
import util.CombinationGenerator;
import util.Pair;
import util.RenameWrapper;

import java.util.*;
import java.util.function.Function;

/**
 * Created by clwang on 9/20/16.
 * An utility funciton for enumeration of aggregation table nodes.
 */
public class AggrEnumerator {

    /**
     * Given an enumeration context, foreach tableNode in the context,
     * enumerate all possible aggregation tablenodes from these queries
     * @param ec Enumeration context containing core tableNodes as well as aggregators to be used.
     * @param simplify Whether allowing multiple aggregation targets in different columns of the same tables.
     * @return The list of aggregation nodes enumerated from ec.
     */
    public static List<TableNode> enumAggrFromEC(EnumContext ec, boolean simplify) {
        List<TableNode> result = new ArrayList<>();
        for (TableNode core : ec.getTableNodes()) {
            result.addAll(enumerateAggregation(ec, core, simplify));
        }
        return result;
    }

    public static List<RenameTableNode> enumerateAggregation(
            EnumContext ec,
            TableNode tn,
            boolean simplify) {
        return enumerateAggregation(ec.getAllAggrFuns(), tn, simplify);
    }

        /**
         * Filters are not considered here, enumerating aggregation with filters require a stand alone pipeline.
         * @param tn the table to perform aggregation on
         * @return the list of enumerated table based on the given tablenode
         */
    public static List<RenameTableNode> enumerateAggregation(
            List<Function<List<Value>, Value>> aggrFuns,
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
                RenameTableNode query = (RenameTableNode) RenameWrapper
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

                // in this case, aggregation will not provide any useful information, except count
                if (groupByFields.contains(targetField) && groupByFields.size() == 1) {
                    if (EnumContext.getAggrFuns(targetType, aggrFuns).contains(AggregationNode.AggrCount)) {
                        targetFuncList.add(new Pair<>(targetField, AggregationNode.AggrCount));
                    }
                    continue;
                }

                // Find the target and the aggregation fields now, start generation
                List<Function<List<Value>, Value>> aggrFuncs = EnumContext.getAggrFuns(targetType, aggrFuns);

                // Last step, enumerate all group-by functions
                for (Function<List<Value>, Value> f : aggrFuncs) {
                    targetFuncList.add(new Pair<>(targetField, f));
                }
            }

            if (! simplify) {
                // allow comparison between different rows, since only one table is added to the result
                // the table with allowing multiple table
                aggrNodes.add((RenameTableNode) RenameWrapper.tryRename(new AggregationNode(tn, groupByFields, targetFuncList)));
            } else {
                // each aggregation function will create only one field
                for (Pair<String, Function<List<Value>, Value>> p : targetFuncList) {
                    // we perform a renaming to ensure consistency for other parts
                    RenameTableNode rt = (RenameTableNode) RenameWrapper
                            .tryRename(new AggregationNode(tn, groupByFields, Arrays.asList(p)));

                    // add the aggregation query without eval in to the result
                    aggrNodes.add(rt);
                }
            }
        }
        return aggrNodes;
    }
}
