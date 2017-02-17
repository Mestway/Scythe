package forward_enumeration.primitive;

import forward_enumeration.context.EnumContext;
import global.GlobalConfig;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.LeftJoinNode;
import sql.lang.ast.table.TableNode;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import util.CombinationGenerator;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The enumerator that enumerates left-join queries
 * Created by clwang on 10/6/16.
 */
public class LeftJoinEnumerator {

    public static List<LeftJoinNode> enumLeftJoinFromEC(EnumContext ec) {
        List<LeftJoinNode> result = new ArrayList<>();
        List<TableNode> baseTableNodes = ec.getTableNodes();
        for (int i = 0; i < baseTableNodes.size(); i ++) {
            for (int j = i; j < baseTableNodes.size(); j ++) {
                result.addAll(enumLeftJoin(baseTableNodes.get(i), baseTableNodes.get(j)));
            }
        }
        return result;
    }

    public static List<LeftJoinNode> enumLeftJoin(TableNode tn1, TableNode tn2) {

        List<LeftJoinNode> leftJoinResult = new ArrayList<>();

        // infer join keys
        List<Pair<String, ValType>> t1Columns = tn1.getSchemaWithType();
        List<Pair<String, ValType>> t2Columns = tn2.getSchemaWithType();

        Table t1, t2;

        try {
            t1 = tn1.eval(new Environment());
            t2 = tn2.eval(new Environment());
        } catch (SQLEvalException e) {
            System.out.println("[ERROR@LeftJoinEnumerator32] TableNodes unable to be evaluated.");
            t1 = null;
            t2 = null;
        }

        List<Pair<String, String>> joinKeys = new ArrayList<>();
        for (Pair<String, ValType> ct1 : t1Columns) {
            for (Pair<String, ValType> ct2 : t2Columns) {

                if (!ct1.getValue().equals(ct2.getValue()))
                    continue;

                boolean existsIntersection = false;
                boolean allContained = true;
                List<Value> col1 = t1.getColumnByIndex(t1.retrieveIndex(ct1.getKey()));
                List<Value> col2 = t2.getColumnByIndex(t2.retrieveIndex(ct2.getKey()));
                for (Value v : col1) {
                    if (col2.contains(v))
                        existsIntersection = true;
                    if (!col2.contains(v))
                        allContained = false;
                }

                // we will not generate left-join queries for columns that have no intersection values,
                // or columns that are already fully contained.
                if ((! existsIntersection))
                    continue;

                joinKeys.add(new Pair<>(ct1.getKey(), ct2.getKey()));
            }
        }

        // TODO: work out a better filtering function to correctly handle the case, see the test case.

        // iterating over the length of join keys to generate left-join nodes for all tables.
        for (int keyLength = 1; keyLength <= GlobalConfig.LEFT_JOIN_KEY_LENGTH; keyLength ++) {
            // generate combinations of the join keys, in support of queries with multiple join keys
            List<List<Pair<String, String>>> combinations = CombinationGenerator
                    .genCombination(joinKeys, keyLength)
                    .stream()
                    .filter(l -> {
                        int nonDupCount1 = l.stream().map(p -> p.getKey()).collect(Collectors.toSet()).size();
                        int nonDupCount2 = l.stream().map(p -> p.getKey()).collect(Collectors.toSet()).size();
                        return (nonDupCount1 == l.size() && nonDupCount2 == l.size());
                    }).collect(Collectors.toList());

            for (List<Pair<String, String>> l : combinations) {
                leftJoinResult.add(new LeftJoinNode(tn1, tn2, l));
            }
        }

        return leftJoinResult;
    }

}
