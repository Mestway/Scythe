package scythe_interface;

import forward_enumeration.table_enumerator.AbstractTableEnumerator;
import global.GlobalConfig;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.table.UnionNode;
import sql.lang.exception.SQLEvalException;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 3/22/16.
 */
public class Synthesizer {

    public static long TimeOut = 600000;

    public static List<TableNode> Synthesize(String exampleFilePath, AbstractTableEnumerator enumerator) {

        // read file
        ExampleDS exampleDS = ExampleDS.readFromFile(exampleFilePath);
        System.out.println("[[Synthesis start]]");
        System.out.println("\tFile: " + exampleFilePath);
        System.out.println("\tEnumerator: " + enumerator.getClass().getSimpleName());

        long timeUsed = 0;
        long timeStart = System.currentTimeMillis();

        List<TableNode> candidates = new ArrayList<>();

        int maxDepth = 1;
        while (candidates.isEmpty() && timeUsed < Synthesizer.TimeOut) {
            System.out.println("[Retry] Maximum Depth: " + maxDepth);

            if (maxDepth == 2)
                System.out.println(exampleDS.output);


            exampleDS.enumConfig.setMaxDepth(maxDepth);
            // synthesize
            candidates.addAll(enumerator.enumProgramWithIO(exampleDS.inputs, exampleDS.output, exampleDS.enumConfig));

            if (!candidates.isEmpty())
                continue;

            if (maxDepth == 1 && exampleDS.output.getContent().size() <= GlobalConfig.TRY_DECOMPOSE_ROW_NUM) {
                // try decomposing the output table
                for (Pair<Table, Table> decomposed : Table.tryDecompose(exampleDS.output)) {

                    exampleDS.enumConfig.setMaxDepth(1);
                    List<TableNode> left = enumerator.enumProgramWithIO(exampleDS.inputs, decomposed.getKey(), exampleDS.enumConfig);

                    exampleDS.enumConfig.setMaxDepth(1);
                    List<TableNode> right = enumerator.enumProgramWithIO(exampleDS.inputs, decomposed.getValue(), exampleDS.enumConfig);
                    for (TableNode tn1 : left) {
                        for (TableNode tn2 : right) {
                            candidates.add(new UnionNode(Arrays.asList(tn1, tn2)));
                        }
                    }

                    if (candidates.size() > GlobalConfig.MAXIMUM_QUERY_KEPT) {
                        candidates.sort((tn1, tn2) -> Double.compare(tn1.estimateAllFilterCost(), tn2.estimateAllFilterCost()));
                        candidates = candidates.subList(0, GlobalConfig.MAXIMUM_QUERY_KEPT);
                    }
                      //  break;
                }
            }

            if (candidates.size() > 0)
                break;

            if (maxDepth == 2) {
                // try synthesizing queries with Exists-clauses
                for (Table existsCore : exampleDS.inputs) {
                    exampleDS.enumConfig.setExistsCore(2, Arrays.asList(existsCore));
                    exampleDS.enumConfig.setMaxDepth(maxDepth - 1);
                    // synthesize

                    candidates.addAll(enumerator.enumProgramWithIO(exampleDS.inputs, exampleDS.output, exampleDS.enumConfig));
                }
            }

            exampleDS.enumConfig.setExistsCore(0, new ArrayList<>());
            exampleDS.enumConfig.setMaxDepth(maxDepth);

            timeUsed = System.currentTimeMillis() - timeStart;
            maxDepth ++;
        }

        int count = 0;
        candidates.sort((tn1, tn2) -> Double.compare(tn1.estimateAllFilterCost(), tn2.estimateAllFilterCost()));
        for (TableNode tn : candidates) {
            try {
                Table t = tn.eval(new Environment());
                if( count >= GlobalConfig.MAXIMUM_QUERY_KEPT) break;
                System.out.println("[No." + (count + 1) + "]===============================");
                count ++;
                System.out.println(tn.prettyPrint(0));
                System.out.println(t);
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }

        // formatting time
        timeUsed = System.currentTimeMillis() - timeStart;
        long second = (timeUsed / 1000) % 60;
        long minute = (timeUsed / (1000 * 60)) % 60;
        long hour = (timeUsed / (1000 * 60 * 60)) % 24;
        String time = String.format("%02d:%02d:%02d:%d", hour, minute, second, timeUsed % 1000);

        System.out.println("[[Synthesizer finished]] time: " + time);
        System.out.printf("[[Synthesizer finished]] seconds: %.5f\n", (minute*60. + second + 0.001 * (timeUsed % 1000)));

        return candidates;
    }
}
