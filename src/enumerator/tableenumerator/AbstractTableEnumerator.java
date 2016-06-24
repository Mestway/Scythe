package enumerator.tableenumerator;

import enumerator.Constraint;
import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import enumerator.context.TableTreeNode;
import enumerator.parameterized.EnumParamTN;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.CostEstimator;

import javax.management.Query;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public abstract class AbstractTableEnumerator {

    public List<TableNode> enumProgramWithIO(List<Table> input, Table output, Constraint c) {

        System.out.println("[Initialize Enumeration]");

        List<ValNode> vns = new ArrayList<>();
        vns.addAll(c.constValNodes());

        List<TableNode> parameterizedTables = EnumParamTN
                .enumParameterizedTableNodes(
                        input.stream()
                                .map(t -> new NamedTable(t)).collect(Collectors.toList()),
                        vns,
                        c.getNumberOfParam());

        System.out.println("  Parameterized Table Number: " + parameterizedTables.size());

        EnumContext ec = new EnumContext(input, c);
        ec.setParameterizedTables(parameterizedTables);
        ec.setOutputTable(output);
        ec.setMaxFilterLength(c.maxFilterLength());

        System.out.println("[Enumeration Start]");

        QueryChest qc = this.enumTable(ec, c.maxDepth());

        List<TableNode> valid = qc.lookup(output);

        if (valid == null) {
            System.out.println("[Enumeration Finished] Does not find a query in the search space.");
            return valid;
        } else {
            List<TableNode> tss = qc.lookup(output);
            List<TableNode> result = new ArrayList<>();
            /*for (TableNode ts : tss) {
                result.addAll(qc.export(ts, new ArrayList<>(),
                        input.stream().map(i -> new NamedTable(i)).collect(Collectors.toList())));
            }*/

            System.out.println("[Consistent Table number] " + qc.getEdges().getDirectLinkCount(ec.getOutputTable()));

            Set<Table> leafNodes = new HashSet<>(); leafNodes.addAll(ec.getInputs());
            List<TableTreeNode> trees = qc.getEdges().findTableTrees(ec.getOutputTable(), leafNodes, 4);

            int totalQueryCount = 0;

            List<TableNode> synthesisResult = new ArrayList<>();

            for (TableTreeNode t : trees) {
                //System.out.println("--------------------------");
                t.inferQuery(ec);
                List<TableNode> tns = t.treeToQuery(ec);

                tns.sort(new Comparator<TableNode>() {
                    @Override
                    public int compare(TableNode o1, TableNode o2) {
                        double c1 = CostEstimator.estimateTableNodeCost(o1, ec);
                        double c2 = CostEstimator.estimateTableNodeCost(o2, ec);
                        if (c1 < c2) {
                            return -1;
                        } else if (c1 == c2) {
                            return 0;
                        } else return 1;
                    }
                });

                tns = tns.subList(0, tns.size() > 10 ? 10 : tns.size());

                int count = t.countQueryNum();
                totalQueryCount += count;

                //t.print(0);

                synthesisResult.addAll(tns);

                //System.out.println("--------------------------");
            }

            synthesisResult.sort(new Comparator<TableNode>() {
                @Override
                public int compare(TableNode o1, TableNode o2) {
                    double c1 = CostEstimator.estimateTableNodeCost(o1, ec);
                    double c2 = CostEstimator.estimateTableNodeCost(o2, ec);
                    if (c1 < c2) {
                        return -1;
                    } else if (c1 == c2) {
                        return 0;
                    } else return 1;
                }
            });

            for (int i = 0; i < 20; i ++) {
                if (i >= synthesisResult.size()) break;
                System.out.println("==== [No. " + i + " ] ====");
                System.out.println(synthesisResult.get(i).prettyPrint(0));
            }

            System.out.println("Total Tree Count: " + trees.size());
            System.out.println("Total Query Count: " + totalQueryCount);
            System.out.println("[Enumeration Finished]");
            return result;
        }
    }

    // the enumeration result will be stored in EC
    abstract public QueryChest enumTable(EnumContext ec, int depth);

}