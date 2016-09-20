package forward_enumeration.primitive;

import forward_enumeration.context.EnumContext;
import forward_enumeration.context.QueryChest;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import util.Pair;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
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
    public static final boolean SIMPLIFY = true;

    /***********************************************************
     * Enum by Aggregation
     *  1. Enumerate the group-by fields
     *      1) should be able to do some real aggregation
     *  2. Enumerate the target field
     *  3. Enumerate the aggregation function
     *      2) based on the type of the target field
     ***********************************************************/

    public static List<TableNode> enumAggregationNode(EnumContext ec) {
        return enumAggregationNodeFlag(ec, SIMPLIFY, true);
    }

    public static List<TableNode> enumAggregationNodeFlag(EnumContext ec, boolean simplify, boolean withFilter) {

        List<TableNode> coreTableNodes = ec.getTableNodes();

        List<TableNode> aggregationNodes = new ArrayList<TableNode>();
        for (TableNode coreTable : coreTableNodes) {
            aggregationNodes.addAll(enumAggrPerTable(ec, coreTable, simplify, withFilter));
        }

        return aggregationNodes;
    }


    // the following two are functions for emit enumerating the tables.
    public static void emitEnumAggregationNode(EnumContext ec, QueryChest qc) {
        List<TableNode> coreTableNodes = ec.getTableNodes();
        for (TableNode coreTable : coreTableNodes) {
            generalEnumAggrPerTable(ec, coreTable, SIMPLIFY, Optional.of(qc), true);
        }
    }

    private static List<TableNode> enumAggrPerTable(
            EnumContext ec,
            TableNode tn,
            boolean simplify,
            boolean withFilter) {
        return generalEnumAggrPerTable(ec, tn, simplify, Optional.ofNullable(null), withFilter);
    }

    /**
     * Filters are not considered here, enumerating aggregation with filters require a stand alone pipeline.
     * @param tn the table to perform aggregation on
     * @return the list of enumerated table based on the given tablenode
     */
    private static List<TableNode> generalEnumAggrPerTable(
            EnumContext ec,
            TableNode tn,
            boolean simplify,
            Optional<QueryChest> optionalQC,
            boolean withFilter) {

        List<TableNode> aggrNodes = new ArrayList<>();

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
                if (optionalQC.isPresent()) {
                    emitToQueryChest(query, tn, optionalQC.get());
                } else {
                    aggrNodes.add(query);
                }
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
                // allow comparison between different rows
                AggregationNode an = new AggregationNode(tn, groupByFields, targetFuncList);

                // the table with allowing multiple table
                if (optionalQC.isPresent()) {
                    emitToQueryChest(an, tn, optionalQC.get());
                } else {
                    aggrNodes.add(an);
                }

                // allowing comparing between aggregation fields with anything else
                if (withFilter) {
                    List<TableNode> wrappedWithFilter = new ArrayList<>();
                    wrappedWithFilter.add(an);

                    TableNode renamedAggrNode = RenameTNWrapper.tryRename(an);
                    Map<String, ValType> typeMap = new HashMap<>();
                    for (int i = 0; i < renamedAggrNode.getSchema().size(); i ++) {
                        typeMap.put(renamedAggrNode.getSchema().get(i),
                                renamedAggrNode.getSchemaType().get(i));
                    }

                    EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

                    List<ValNode> L = new ArrayList<>();
                    for (int i = 0; i < targetFuncList.size(); i ++) {
                        L.add(new NamedVal(renamedAggrNode.getSchema().get(i + groupByFields.size())));
                    }
                    List<ValNode> R = ec2.getValNodes();

                    // we don't want to have EXISTS filters used in filtering aggregation
                    boolean allowExists = false;
                    List<Filter> filters = FilterEnumerator.enumFiltersLR(L, R, ec2, allowExists);
                    for (Filter f : filters) {
                        wrappedWithFilter.add(
                                new SelectNode(
                                    renamedAggrNode.getSchema()
                                        .stream().map(v -> new NamedVal(v)).collect(Collectors.toList()),
                                    renamedAggrNode, f
                                ));
                    }

                    // emitting or adding them to the collector set
                    for (TableNode wrappedTn : wrappedWithFilter) {
                        if (optionalQC.isPresent()) {
                            emitToQueryChest(wrappedTn, tn, optionalQC.get());
                        } else {
                            aggrNodes.add(wrappedTn);
                        }
                    }
                }
            } else {
                for (Pair<String, Function<List<Value>, Value>> p : targetFuncList) {
                    //aggrNodes.add(new AggregationNode(tn, aggrFields, Arrays.asList(p)));
                    RenameTableNode rt = (RenameTableNode) RenameTNWrapper
                            .tryRename(new AggregationNode(tn, groupByFields, Arrays.asList(p)));

                    if (withFilter) {
                        // filters for aggregation fields are listed here
                        List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterAggrNode(rt, ec);
                        for (Filter f : filters) {
                            List<ValNode> vals = rt.getSchema().stream()
                                    .map(s -> new NamedVal(s))
                                    .collect(Collectors.toList());
                            TableNode filtered = RenameTNWrapper.tryRename(new SelectNode(vals, rt, f));

                            if (optionalQC.isPresent()) {
                                emitToQueryChest(filtered, tn, optionalQC.get());
                            } else {
                                aggrNodes.add(filtered);
                            }
                        }
                    }

                    // add the aggregation query without filter in to the query chest
                    if (optionalQC.isPresent()) {
                        emitToQueryChest(rt, tn, optionalQC.get());
                    } else {
                        aggrNodes.add(rt);
                    }
                }
            }
        }
        return aggrNodes;
    }

    private static void emitToQueryChest(TableNode result, TableNode original, QueryChest qc) {
        qc.insertQuery(result);
        try {
            // updating the link between tables, an edge eval(tn) --> eval(rt) is inserted
            qc.getTableLinks().insertEdge(
                    qc.getRepresentative(original.eval(new Environment())),
                    qc.getRepresentative(result.eval(new Environment())));
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }

}
