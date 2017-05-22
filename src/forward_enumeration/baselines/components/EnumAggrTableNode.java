package forward_enumeration.baselines.components;

import forward_enumeration.context.EnumContext;
import forward_enumeration.QueryContainer;
import forward_enumeration.enum_components.FilterEnumerator;
import global.GlobalConfig;
import lang.table.Table;
import lang.sql.ast.predicate.Predicate;
import lang.sql.ast.contable.RenameTableNode;
import lang.sql.ast.contable.SelectNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.ValNode;
import lang.sql.ast.Environment;
import lang.sql.exception.SQLEvalException;
import util.RenameWrapper;
import forward_enumeration.enum_components.AggrEnumerator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumerate aggregation nodes with provided enum context
 * Created by clwang on 1/7/16.
 */
public class EnumAggrTableNode {

    // When this flag is true, we will not allow comparison between multiple aggregation fields
    public static final boolean SIMPLIFY = GlobalConfig.SIMPLIFY_AGGR_FIELD;

    /***********************************************************
     * Enum by Aggregation
     *  1. Enumerate the group-by fields
     *      1) should be able to do some real aggregation
     *  2. Enumerate the target field
     *  3. Enumerate the aggregation function
     *      2) based on the type of the target field
     ***********************************************************/

    public static List<TableNode> enumAggrNodeWFilter(EnumContext ec) {

        boolean simplify = SIMPLIFY;
        boolean withFilter = true;

        List<TableNode> coreTableNodes = ec.getTableNodes();

        List<TableNode> aggregationNodes = new ArrayList<TableNode>();
        for (TableNode coreTable : coreTableNodes) {
            aggregationNodes.addAll(generalEnumAggrPerTable(ec, coreTable, simplify, Optional.ofNullable(null), withFilter));
        }

        return aggregationNodes;
    }

    // the following two are functions for emit enumerating the tables.
    public static List<Table> emitEnumAggrNodeWFilter(EnumContext ec, QueryContainer qc) {

        Set<Table> newlyGeneratedTables = new HashSet<>();

        List<TableNode> coreTableNodes = ec.getTableNodes();
        for (TableNode coreTable : coreTableNodes) {
            List<RenameTableNode> aggrNodes = AggrEnumerator.enumerateAggregation(ec, coreTable, SIMPLIFY);
            for (RenameTableNode rt : aggrNodes) {

                // filters for aggregation fields are listed here
                List<TableNode> result = new ArrayList<>();
                List<Predicate> filters = FilterEnumerator.enumCanonicalFilterAggrNode(rt, ec);
                for (Predicate f : filters) {
                    List<ValNode> vals = rt.getSchema().stream()
                            .map(s -> new NamedVal(s))
                            .collect(Collectors.toList());
                    TableNode filtered = RenameWrapper.tryRename(new SelectNode(vals, rt, f));
                    result.add(filtered);
                }

                for (TableNode tn : result) {
                    try {
                        Table resultT = tn.eval(new Environment());
                        Table originalT = coreTable.eval(new Environment());

                        if (originalT.getContent().isEmpty() || resultT.getContent().isEmpty())
                            continue;

                        qc.insertQuery(tn);

                        // updating the link between tables, an edge eval(tn) --> eval(rt) is inserted
                        qc.getTableLinks().insertEdge(
                                qc.getRepresentative(originalT),
                                qc.getRepresentative(resultT));

                        newlyGeneratedTables.add(qc.getRepresentative(resultT));

                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return newlyGeneratedTables.stream().collect(Collectors.toList());
    }

    /**
     * Filters are not considered here, enumerating aggregation with filters require a stand alone pipeline.
     * @param tn the table to perform aggregation on
     * @return the list of enumerated table based on the given tablenode if optionalQC is not provided
     */
    private static List<TableNode> generalEnumAggrPerTable(
            EnumContext ec,
            TableNode tn,
            boolean simplify,
            Optional<QueryContainer> optionalQC,
            boolean withFilter) {

        List<TableNode> result = new ArrayList<>();
        List<RenameTableNode> aggrNodes = AggrEnumerator.enumerateAggregation(ec, tn, simplify);

        if (withFilter) {

            for (RenameTableNode rt : aggrNodes) {
                // filters for aggregation fields are listed here
                List<Predicate> filters = FilterEnumerator.enumCanonicalFilterAggrNode(rt, ec);
                for (Predicate f : filters) {
                    List<ValNode> vals = rt.getSchema().stream()
                            .map(s -> new NamedVal(s))
                            .collect(Collectors.toList());
                    TableNode filtered = RenameWrapper.tryRename(new SelectNode(vals, rt, f));

                    result.add(filtered);
                }
            }
        } else {
            result = aggrNodes.stream().map(x -> x).collect(Collectors.toList());
        }

        // a possible way to speedup this process is to make this emission process into the enumeration process
        if (optionalQC.isPresent()) {
            for (TableNode x : result) {
                emitToQueryChest(x, tn, optionalQC.get());
            }
        }

        return result;
    }

    private static void emitToQueryChest(TableNode result, TableNode original, QueryContainer qc) {

        try {
            Table resultT = result.eval(new Environment());
            Table originalT = original.eval(new Environment());

            if (originalT.getContent().isEmpty() || resultT.getContent().isEmpty())
                return;

            qc.insertQuery(result);

            // updating the link between tables, an edge eval(tn) --> eval(rt) is inserted
            qc.getTableLinks().insertEdge(
                    qc.getRepresentative(originalT),
                    qc.getRepresentative(resultT));
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }

}
