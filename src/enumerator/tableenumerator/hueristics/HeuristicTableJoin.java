package enumerator.tableenumerator.hueristics;

import com.sun.tools.javac.util.Pair;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.RenameTNWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 2/20/16.
 */
public class HeuristicTableJoin {

    public static TableNode equiJoin(List<Table> tables) {
        List<Boolean> used = new ArrayList<>();
        for (int i = 0; i < tables.size(); i ++) {
            used.add(false);
        }
        boolean stable = false;
        used.set(0, true);
        TableNode result = new NamedTable(tables.get(0));
        while(!stable) {
            stable = true;
            for (int i = 0; i < tables.size(); i ++) {
                if (used.get(i)) continue;
                Pair<Boolean,TableNode> tryJoin = equiJoinTwo(result, new NamedTable(tables.get(i)));
                if (tryJoin.fst) {
                    used.set(i, true);
                    result = tryJoin.snd;
                    stable = false;
                }
            }
        }
        for (int i = 0; i < tables.size(); i ++) {
            if (used.get(i) == false) {
                result = RenameTNWrapper.tryRename(
                        new JoinNode(Arrays.asList(result, new NamedTable(tables.get(i)))));
            }
        }

        return result;
    }

    /**
     * try to infer the equi join key between two tables tn1 and tn2
     * @param tn1 table 1
     * @param tn2 table 2
     * @return a pair, the first element indicates whether we successfully find a equi join
     *          the second is the join result -- equi join result if succeed, other wise return the cartesian product
     */
    public static Pair<Boolean, TableNode> equiJoinTwo(TableNode tn1, TableNode tn2) {
        Table t1, t2;
        try {
            t1 = tn1.eval(new Environment());
            t2 = tn2.eval(new Environment());
        } catch (SQLEvalException e) {
            t1 = null;
            t2 = null;
            e.printStackTrace();
        }

        for (int i = 0; i < t1.getMetadata().size(); i ++) {
            for (int j = 0; j < t2.getMetadata().size(); j ++) {
                if (t1.getSchemaType().get(i) == t2.getSchemaType().get(j)) {
                    int ind1 = i, ind2 = j;
                    List<Value> vals1 = t1.getContent()
                                            .stream()
                                            .map(c -> c.getValue(ind1))
                                            .collect(Collectors.toList());
                    List<Value> vals2 = t2.getContent()
                                            .stream()
                                            .map(c -> c.getValue(ind2))
                                            .collect(Collectors.toList());
                    int score = 0;
                    for (int k1 = 0; k1 < vals1.size(); k1 ++) {
                        for (int k2 = 0; k2 < vals2.size(); k2++) {
                            if (vals1.get(k1).equals(vals2.get(k2))) {
                                score++;
                            }
                        }
                    }
                    if ((score * 10) / ((vals1.size() + vals2.size()) /2) > 2) {
                        TableNode renamedJoin = RenameTNWrapper
                                .tryRename(new JoinNode(Arrays.asList(tn1, tn2)));

                        List<ValNode> newSchema = new ArrayList<>();
                        for (int k = 0; k < renamedJoin.getSchema().size(); k ++) {
                            if (k != j + t1.getMetadata().size()) {
                                newSchema.add(new NamedVal(renamedJoin.getSchema().get(k)));
                            }
                        }
                        return new Pair<Boolean, TableNode>(true,new SelectNode(
                                newSchema,
                                renamedJoin,
                                new VVComparator(
                                        Arrays.asList(
                                            new NamedVal(renamedJoin.getSchema().get(i)),
                                            new NamedVal(renamedJoin.getSchema().get(t1.getMetadata().size() + j))),
                                        VVComparator.eq
                                )));
                    }
                }
            }
        }
        return new Pair<Boolean, TableNode>(false, RenameTNWrapper.tryRename(new JoinNode(Arrays.asList(tn1, tn2))));
    }
}
