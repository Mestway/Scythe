package forward_enumeration.baselines.components;

import forward_enumeration.QueryContainer;
import forward_enumeration.context.EnumContext;
import forward_enumeration.enum_components.LeftJoinEnumerator;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.contable.LeftJoinNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.exception.SQLEvalException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 10/22/16.
 */
public class EnumLeftJoin {


    public static List<LeftJoinNode> enumLeftJoin(EnumContext ec) {
        return LeftJoinEnumerator.enumLeftJoinFromEC(ec);
    }

    public static List<Table> emitEnumLeftJoin(
            List<TableNode> leftSet,
            List<TableNode> rightSet,
            QueryContainer qc) {

        List<Table> newlyGeneratedTable = new ArrayList<>();

        for (int i = 0; i < leftSet.size(); i ++) {
            for (int j = 0; j < rightSet.size(); j ++) {
                List<LeftJoinNode> ljns = LeftJoinEnumerator.enumLeftJoin(leftSet.get(i), rightSet.get(j));

                for (TableNode ljn : ljns) {
                    try {
                        Table ljnt = ljn.eval(new Environment());
                        if (qc != null) {
                            ljnt = qc.getRepresentative(ljnt);
                            qc.insertQuery(ljn);
                            qc.getTableLinks().insertEdge(
                                    qc.getRepresentative(leftSet.get(i).eval(new Environment())),
                                    qc.getRepresentative(rightSet.get(j).eval(new Environment())),
                                    ljnt);
                        }
                        newlyGeneratedTable.add(ljnt);
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return newlyGeneratedTable;
    }
}
