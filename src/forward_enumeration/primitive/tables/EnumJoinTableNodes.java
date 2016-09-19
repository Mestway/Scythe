package forward_enumeration.primitive.tables;

import forward_enumeration.context.EnumContext;
import forward_enumeration.context.QueryChest;
import forward_enumeration.primitive.EnumCanonicalFilters;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.CombinationGenerator;
import util.RenameTNWrapper;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Enumerate join table nodes with given EC
 * Created by clwang on 1/7/16.
 */
public class EnumJoinTableNodes {

    /**
     * The general emit join node function, other emit function are supposed to be implemented around this one.
     * @param tableNum the number of tables should appear in the join table body
     * @param ec the enumeration context
     * @param qc the query chest for emission
     * @param checker the checker to check whether a table list is valid or not
     * @param withFilter whether filter should be included
     */
    public static void generalEmitEnumJoin(
            int tableNum,
            EnumContext ec,
            QueryChest qc,
            BiFunction<EnumContext, List<TableNode>, Boolean> checker,
            boolean withFilter) {

        List<TableNode> basicTables = ec.getTableNodes();

        for (TableNode ti : basicTables) {
            for (TableNode tj : basicTables) {

                List<TableNode> tns = Arrays.asList(ti, tj);

                if (! checker.apply(ec,tns) ) {
                    continue;
                }

                JoinNode jn = new JoinNode(tns);

                if (withFilter == false) {
                    qc.updateQuery(jn);
                    try {
                        qc.getEdges().insertEdge(
                                qc.representative(tns.get(0).eval(new Environment())),
                                qc.representative(tns.get(1).eval(new Environment())),
                                qc.representative(jn.eval(new Environment())));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                } else {
                    RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
                    // add the query without join
                    qc.updateQuery(rt);
                    try {
                        qc.getEdges().insertEdge(
                                qc.representative(tns.get(0).eval(new Environment())),
                                qc.representative(tns.get(1).eval(new Environment())),
                                qc.representative(rt.eval(new Environment())));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }

                    List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterJoinNode(rt, ec);
                    for (Filter f : filters) {
                        // the selection args are complete
                        List<ValNode> vals = rt.getSchema().stream()
                                .map(s -> new NamedVal(s))
                                .collect(Collectors.toList());
                        TableNode resultTn = new SelectNode(vals, rt, f);
                        qc.updateQuery(RenameTNWrapper.tryRename(resultTn));
                        try {
                            qc.getEdges().insertEdge(
                                    qc.representative(tns.get(0).eval(new Environment())),
                                    qc.representative(tns.get(1).eval(new Environment())),
                                    qc.representative(resultTn.eval(new Environment())));
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    //Similar to general emit enum join, except that tables are not emitted on the fly
    public static List<TableNode> generalEnumJoin(
            int tableNum,
            EnumContext ec,
            BiFunction<EnumContext, List<TableNode>, Boolean> checker,
            boolean withFilter) {

        List<TableNode> result = new ArrayList<>();

        List<TableNode> basicTables = ec.getTableNodes();

        // table combinations
        List<List<TableNode>> tableComb = CombinationGenerator.genMultPermutation(basicTables, tableNum);

        for (List<TableNode> tns : tableComb) {
            if (!checker.apply(ec, tns))
                continue;
            JoinNode jn = new JoinNode(tns);

            if (withFilter == false) {
                result.add(jn);
            }else {
                RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
                // add the query without join
                result.add(rt);

                List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterJoinNode(rt, ec);
                for (Filter f : filters) {
                    // the selection args are complete
                    List<ValNode> vals = rt.getSchema().stream()
                            .map(s -> new NamedVal(s))
                            .collect(Collectors.toList());
                    result.add(RenameTNWrapper.tryRename(new SelectNode(vals, rt, f)));
                }
            }
        }

        return result;
    }

    /*****************************************************
     Enumeration by join
     1. Enumerate atomic tables and then do join
     *****************************************************/

    public static void emitEnumJoinWithoutFilter(EnumContext ec, QueryChest qc) {
        EnumJoinTableNodes.generalEmitEnumJoin(2, ec, qc, atMostOneNoneInput, false);
    }

    public static void emitEnumJoinWithFilter(EnumContext ec, QueryChest qc) {
        EnumJoinTableNodes.generalEmitEnumJoin(2, ec, qc, atMostOneNoneInput, true);
    }

    // This is a simpler version of joining considering no filters at this stage,
    // Joining is only a matter of performing cartesian production here.
    public static List<JoinNode> enumJoinWithoutFilter(EnumContext ec) {
        List<TableNode> tns = generalEnumJoin(2, ec, atMostOneNoneInput, false);
        return tns.stream().map(tn -> (JoinNode) tn).collect(Collectors.toList());
    }

    // This is the join we used in canonicalSQL,
    // filters are used in enumerating canonical join nodes.
    public static List<TableNode> enumJoinWithFilter(EnumContext ec) {
        return generalEnumJoin(2, ec, atMostOneNoneInput, true);
    }

    public static final BiFunction<EnumContext, List<TableNode>, Boolean> atMostOneNoneInput = (ec, lst) -> {
        for (int i = 1; i < lst.size(); i ++) {
            if (! ec.isInputTableNode(lst.get(i)))
                return false;
        }
        //if (lst.get(0) == lst.get(1)) return false;
        return true;
    };

}
