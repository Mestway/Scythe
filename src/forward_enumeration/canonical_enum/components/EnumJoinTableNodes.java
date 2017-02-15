package forward_enumeration.canonical_enum.components;

import forward_enumeration.context.EnumContext;
import forward_enumeration.container.QueryContainer;
import forward_enumeration.primitive.FilterEnumerator;
import sql.lang.Table;
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

    public static List<TableNode> enumJoinLeftRight(List<TableNode> left, List<TableNode> right, EnumContext ec) {

        List<TableNode> result = new ArrayList<>();

        for (TableNode ti : left) {
            for (TableNode tj : right) {

                if (ti.getTableName() == tj.getTableName()) {
                    tj = RenameTNWrapper.tryRename(tj);
                }

                List<TableNode> tns = Arrays.asList(ti, tj);
                JoinNode jn = new JoinNode(tns);
                RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);

                result.add(rt);

                List<Filter> filters = FilterEnumerator.enumCanonicalFilterJoinNode(rt, ec);
                for (Filter f : filters) {
                    // the selection args are complete
                    List<ValNode> vals = rt.getSchema().stream()
                            .map(s -> new NamedVal(s))
                            .collect(Collectors.toList());

                    TableNode resultTn = new SelectNode(vals, rt, f);

                    try {
                        Table tns0 = tns.get(0).eval(new Environment());
                        Table tns1 = tns.get(1).eval(new Environment());
                        Table resultT = resultTn.eval(new Environment());

                        if (tns0.getContent().isEmpty() || tns1.getContent().isEmpty() || resultT.isEmpty())
                            continue;

                        result.add(resultTn);
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result;
    }

    /**
     * The general emit join node function, other emit function are supposed to be implemented around this one.
     * @param ec the enumeration context
     * @param qc the query chest for emission
     */
    public static List<Table> generalEmitEnumJoin(
            List<TableNode> leftSet,
            List<TableNode> rightSet,
            EnumContext ec,
            QueryContainer qc) {

        List<Table> newlyGeneratedTable = new ArrayList<>();
        
        for (TableNode ti : leftSet) {
            for (TableNode tj : rightSet) {

                List<TableNode> tns = Arrays.asList(ti, tj);

                JoinNode jn = new JoinNode(tns);

                RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
                // add the query without join
                qc.insertQuery(rt);
                try {
                    qc.getTableLinks().insertEdge(
                            qc.getRepresentative(tns.get(0).eval(new Environment())),
                            qc.getRepresentative(tns.get(1).eval(new Environment())),
                            qc.getRepresentative(rt.eval(new Environment())));
                    newlyGeneratedTable.add(qc.getRepresentative(qc.getRepresentative(rt.eval(new Environment()))));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }

                List<Filter> filters = FilterEnumerator.enumCanonicalFilterJoinNode(rt, ec);
                for (Filter f : filters) {

                    // the selection args are complete
                    List<ValNode> vals = rt.getSchema().stream()
                            .map(s -> new NamedVal(s))
                            .collect(Collectors.toList());
                    TableNode resultTn = new SelectNode(vals, rt, f);

                    try {
                        Table tns0 = tns.get(0).eval(new Environment());
                        Table tns1 = tns.get(1).eval(new Environment());
                        Table resultT = resultTn.eval(new Environment());

                        if (tns0.getContent().isEmpty() || tns1.getContent().isEmpty() || resultT.isEmpty()) {
                            continue;
                        }

                        qc.insertQuery(RenameTNWrapper.tryRename(resultTn));

                        qc.getTableLinks().insertEdge(
                                qc.getRepresentative(tns0),
                                qc.getRepresentative(tns1),
                                qc.getRepresentative(resultT));

                        newlyGeneratedTable.add(qc.getRepresentative(resultT));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return newlyGeneratedTable;
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

                List<Filter> filters = FilterEnumerator.enumCanonicalFilterJoinNode(rt, ec);
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
        int noneInputNodeCnt = lst.stream().map(tn -> ec.isInputTableNode(tn) ? 0 : 1).reduce(0, (x,y) -> x + y);
        if (noneInputNodeCnt > 1)
            return false;
        return true;
    };

}
