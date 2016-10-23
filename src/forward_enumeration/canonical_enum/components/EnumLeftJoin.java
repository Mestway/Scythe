package forward_enumeration.canonical_enum.components;

import forward_enumeration.container.QueryContainer;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.LeftJoinEnumerator;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.LeftJoinNode;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

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
                    qc.insertQuery(ljn);
                    try {
                        Table ljnt = qc.getRepresentative(ljn.eval(new Environment()));
                        qc.getTableLinks().insertEdge(
                                qc.getRepresentative(leftSet.get(i).eval(new Environment())),
                                qc.getRepresentative(rightSet.get(j).eval(new Environment())),
                                ljnt);
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
