package enumerator.primitive.tables;

import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import enumerator.primitive.EnumCanonicalFilters;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import util.Pair;
import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.*;
import sql.lang.exception.SQLEvalException;
import util.CombinationGenerator;
import util.RenameTNWrapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumerate aggregation nodes with provided enum context
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

        List<TableNode> coreTableNodes = ec.getTableNodes();

        List<TableNode> aggregationNodes = new ArrayList<TableNode>();
        for (TableNode coreTable : coreTableNodes) {
            aggregationNodes.addAll(enumAggrPerTable(ec, coreTable, SIMPLIFY));
        }

        return aggregationNodes;
    }

    public static List<TableNode> enumAggregationNodeFlag(EnumContext ec, boolean simplify) {

        List<TableNode> coreTableNodes = ec.getTableNodes();

        List<TableNode> aggregationNodes = new ArrayList<TableNode>();
        for (TableNode coreTable : coreTableNodes) {
            aggregationNodes.addAll(enumAggrPerTable(ec, coreTable, simplify));
        }

        return aggregationNodes;
    }

    /**
     * Filters are not considered here, enumerating aggregation with filters require a stand alone pipeline.
     * @param tn the table to perform aggregation on
     * @return the list of enumerated table based on the given tablenode
     */
    private static List<TableNode> enumAggrPerTable(EnumContext ec, TableNode tn, boolean simplify) {

        List<TableNode> aggrNodes = new ArrayList<>();

        List<List<String>> groupByFieldsComb = CombinationGenerator.genCombination(tn.getSchema());

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

            RenameTableNode query = (RenameTableNode) RenameTNWrapper
                    .tryRename(new AggregationNode(tn,
                            groupByFields, new ArrayList<Pair<String, Function<List<Value>, Value>>>()));
            aggrNodes.add(query);

            // If the group by size equals to the schema size, we shall skip the process of finding a target field
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
                // allow comparison between different rows
                AggregationNode an = new AggregationNode(tn, groupByFields, targetFuncList);
                aggrNodes.add(an);

                // the previous version with filters inside
                // I don't think we need to tangle the enumeration for filter here
                /*List<TableNode> wrappedWithFilter = new ArrayList<>();
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
                return wrappedWithFilter;*/

            } else {
                for (Pair<String, Function<List<Value>, Value>> p : targetFuncList) {
                    //aggrNodes.add(new AggregationNode(tn, aggrFields, Arrays.asList(p)));
                    RenameTableNode rt = (RenameTableNode) RenameTNWrapper
                            .tryRename(new AggregationNode(tn, groupByFields, Arrays.asList(p)));

                    // filters for aggregation fields are listed here
                    List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterAggrNode(rt, ec);
                    for (Filter f : filters) {
                        List<ValNode> vals = rt.getSchema().stream()
                                .map(s -> new NamedVal(s))
                                .collect(Collectors.toList());
                        aggrNodes.add(RenameTNWrapper.tryRename(new SelectNode(vals, rt, f)));
                    }
                    aggrNodes.add(rt);
                }
            }
        }
        return aggrNodes;
    }

    public static void emitEnumAggregationNode(EnumContext ec, QueryChest qc) {

        List<TableNode> coreTableNodes = ec.getTableNodes();

        for (TableNode coreTable : coreTableNodes) {
            emitEnumAggrPerTableWithFilter(ec, coreTable, SIMPLIFY, qc);
        }
    }

    /**
     * @param tn the table to perform aggregation on
     * @return the list of enumerated table based on the given tablenode
     */
    private static void emitEnumAggrPerTableWithFilter(
            EnumContext ec, TableNode tn, boolean simplify, QueryChest qc) {

        List<List<String>> groupByComb = CombinationGenerator.genCombination(tn.getSchema());

        for (List<String> groupByFields : groupByComb) {

            // Not sure if this is correct or not
            //TODO: make sure this works in the future, we don't want to group on something that has no effect
            try {
                Table table = tn.eval(new Environment());
                if (AggregationNode.numberOfGroups(table, groupByFields) == table.getContent().size())
                    continue;
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }

            // Part I: add the group by query without aggregation into the group by fields

            RenameTableNode query = (RenameTableNode) RenameTNWrapper
                    .tryRename(new AggregationNode(tn,
                            groupByFields, new ArrayList<Pair<String, Function<List<Value>, Value>>>()));
            qc.updateQuery(query);
            try {
                // updating the link between tables, an edge eval(tn) --> eval(rt) is inserted
                qc.getEdges().insertEdge(tn.eval(new Environment()), query.eval(new Environment()));
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }

            // If the group by size equals to the schema size, we shall skip the process of finding a target field
            // if (groupByFields.size() == tn.getSchema().size())
               // continue;

            // Part II: add queries with both group by and aggregation

            List<Pair<String, Function<List<Value>, Value>>> targetFuncList = new ArrayList<>();

            // Then enum the target fields
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                String targetField = tn.getSchema().get(i);
                ValType targetType = tn.getSchemaType().get(i);

                if (groupByFields.size() == 1 && groupByFields.contains(targetField))
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
                AggregationNode an = new AggregationNode(tn, groupByFields, targetFuncList);
                qc.updateQuery(an);

            } else {
                for (Pair<String, Function<List<Value>, Value>> p : targetFuncList) {
                    RenameTableNode rt = (RenameTableNode) RenameTNWrapper
                            .tryRename(new AggregationNode(tn, groupByFields, Arrays.asList(p)));
                    List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterAggrNode(rt, ec);

                    for (Filter f : filters) {
                        List<ValNode> vals = rt.getSchema().stream()
                                .map(s -> new NamedVal(s))
                                .collect(Collectors.toList());

                        TableNode filtered = RenameTNWrapper.tryRename(new SelectNode(vals, rt, f));
                        qc.updateQuery(filtered);
                        try {
                            qc.getEdges().insertEdge(tn.eval(new Environment()), filtered.eval(new Environment()));
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                    }

                    // add the aggregation query without filter in to the query chest
                    qc.updateQuery(rt);
                    try {
                        // updating the link between tables, an edge eval(tn) --> eval(rt) is inserted
                        qc.getEdges().insertEdge(tn.eval(new Environment()), rt.eval(new Environment()));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
