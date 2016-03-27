package enumerator;

import enumerator.context.EnumContext;
import javafx.util.Pair;
import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.CombinationGenerator;
import util.RenameTNWrapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/7/16.
 */
public class EnumAggrTableNode {

    // When this flag is true, we will not allow comparison between multiple aggregation fields
    private static final boolean SIMPLIFY = true;

    /***********************************************************
     * Enum by Aggregation
     *  1. Enumerate the group-by fields
     *      1) should be able to do some real aggregation
     *  2. Enumerate the target field
     *  3. Enumerate the aggregation function
     *      2) based on the type of the target field
     ***********************************************************/

    public static List<TableNode> enumAggregationNode(EnumContext ec) {

        // currently ignore all table nodes
        List<TableNode> coreTableNodes = ec.getTableNodes().stream().filter(
            t -> {
                if (t instanceof NamedTable)
                    return true;
                return true;
            }
        ).collect(Collectors.toList());

        List<TableNode> aggregationNodes = new ArrayList<TableNode>();
        for (TableNode coreTable : coreTableNodes) {
            aggregationNodes.addAll(enumAggrPerTable(ec, coreTable, SIMPLIFY));
        }

        return aggregationNodes;
    }

    public static List<TableNode> enumAggregationNodeFlag(EnumContext ec, boolean simplify) {

        // currently ignore all complex table nodes (only considering named tables)
        List<TableNode> coreTableNodes = ec.getTableNodes().stream().filter(
                t -> {
                    if (t instanceof NamedTable)
                        return true;
                    return true;
                }
        ).collect(Collectors.toList());

        List<TableNode> aggregationNodes = new ArrayList<TableNode>();
        for (TableNode coreTable : coreTableNodes) {
            aggregationNodes.addAll(enumAggrPerTable(ec, coreTable, simplify));
        }

        return aggregationNodes;
    }

    /**
     *
     * @param tn the table to perform aggregation on
     * @return the list of enumerated table based on the given tablenode
     */
    private static List<TableNode> enumAggrPerTable(EnumContext ec, TableNode tn, boolean simplify) {

        List<TableNode> aggrNodes = new ArrayList<>();

        List<List<String>> aggrFieldsComb = CombinationGenerator.genCombination(tn.getSchema());

        for (List<String> aggrFields : aggrFieldsComb) {

            // we don't want to make the fields have the exact size as the whole schema
            // Jan 14 Chenglong: Probably not
            if (aggrFields.size() == tn.getSchema().size())
                continue;

            // Not sure if this is correct or not
            //TODO: make sure this works in the future, we don't want to group on something that has no effect
            try {
                Table table = tn.eval(new Environment());
                if (AggregationNode.numberOfGroups(table, aggrFields) == table.getContent().size())
                    continue;
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }


            List<Pair<String, Function<List<Value>, Value>>> targetFuncList = new ArrayList<>();

            // Then enum the target fields
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                String targetField = tn.getSchema().get(i);
                ValType targetType = tn.getSchemaType().get(i);

                if (aggrFields.contains(targetField))
                    continue;

                // Find the target and the aggregation fields now, start generation
                List<Function<List<Value>, Value>> aggrFuncs = ec.getAggrFuns(targetType);

                // Last step, enumerate all group-by functions
                for (Function<List<Value>, Value> f : aggrFuncs) {
                    targetFuncList.add(new Pair<>(targetField, f));
                }
            }

            if (! simplify) {
                // allow comparison between different rows
                TableNode an = new AggregationNode(
                        tn, aggrFields, targetFuncList
                );

                List<TableNode> wrappedWithFilter = new ArrayList<>();
                wrappedWithFilter.add(an);

                TableNode renamedAggrNode = RenameTNWrapper.tryRename(an);
                Map<String, ValType> typeMap = new HashMap<>();
                for (int i = 0; i < renamedAggrNode.getSchema().size(); i ++) {
                    typeMap.put(renamedAggrNode.getSchema().get(i),
                            renamedAggrNode.getSchemaType().get(i));
                }
                EnumContext ec2 = EnumContext.extendTypeMap(ec, typeMap);

                List<ValNode> L = new ArrayList<>();
                for (int i = 0; i < targetFuncList.size(); i ++) {
                    L.add(new NamedVal(renamedAggrNode.getSchema().get(i + aggrFields.size())));
                }
                List<ValNode> R = ec2.getValNodes();

                List<Filter> filters = FilterEnumerator.enumFiltersLR(L, R, ec2);
                for (Filter f : filters) {
                    wrappedWithFilter.add(
                            new SelectNode(
                                renamedAggrNode.getSchema()
                                    .stream().map(v -> new NamedVal(v)).collect(Collectors.toList()),
                                renamedAggrNode, f
                            ));
                }

                return wrappedWithFilter;
                // previous unfiltered version
                // aggrNodes.add(an);
            } else {
                for (Pair<String, Function<List<Value>, Value>> p : targetFuncList) {
                    aggrNodes.add(new AggregationNode(tn, aggrFields, Arrays.asList(p)));
                }
            }

        }
        return aggrNodes;
    }

}
