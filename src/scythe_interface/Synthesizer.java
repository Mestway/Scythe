package scythe_interface;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.AggrEnumerator;
import forward_enumeration.table_enumerator.AbstractTableEnumerator;
import global.GlobalConfig;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.RenameTableNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.table.UnionNode;
import sql.lang.datatype.NumberVal;
import sql.lang.exception.SQLEvalException;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

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

        Set<NumberVal> guessedNumConstants = guessingPossibleIntegers(exampleDS.enumConfig, exampleDS.inputs, exampleDS.output);

        exampleDS.enumConfig.addConstVals(guessedNumConstants.stream().collect(Collectors.toSet()));

        int maxDepth = 1;
        while (timeUsed < Synthesizer.TimeOut) {
            System.out.println("[Retry] Maximum Depth: " + maxDepth);

            if (maxDepth == 2)
                System.out.println(exampleDS.output);

            exampleDS.enumConfig.setMaxDepth(maxDepth);
            // synthesize
            candidates.addAll(enumerator.enumProgramWithIO(exampleDS.inputs, exampleDS.output, exampleDS.enumConfig));

            if (containsDesirableCandidate(candidates)) break;

            if (GlobalConfig.TRY_DECOMPOSITION && maxDepth == 1 && exampleDS.output.getContent().size() <= GlobalConfig.TRY_DECOMPOSE_ROW_NUM) {
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
                    // we do not try to break here, since some decompositions may not be desriable
                    // NO BREAK; XXX break;
                }
            }

            if (containsDesirableCandidate(candidates)) break;


            if (maxDepth == 2) {
                // try synthesizing queries with Exists-clauses
                for (Table existsCore : exampleDS.inputs) {
                    exampleDS.enumConfig.setExistsCore(2, Arrays.asList(existsCore));
                    exampleDS.enumConfig.setMaxDepth(maxDepth - 1);
                    // synthesize

                    candidates.addAll(enumerator.enumProgramWithIO(exampleDS.inputs, exampleDS.output, exampleDS.enumConfig));
                    if (containsDesirableCandidate(candidates)) break;
                }
            }

            exampleDS.enumConfig.setExistsCore(0, new ArrayList<>());
            exampleDS.enumConfig.setMaxDepth(maxDepth);

            timeUsed = System.currentTimeMillis() - timeStart;
            maxDepth ++;

            if (containsDesirableCandidate(candidates)) break;
        }

        candidates.sort((tn1, tn2) -> Double.compare(tn1.estimateAllFilterCost(), tn2.estimateAllFilterCost()));
        int lastIndex = GlobalConfig.MAXIMUM_QUERY_KEPT > candidates.size() ? candidates.size() - 1
                : GlobalConfig.MAXIMUM_QUERY_KEPT - 1;
        for (int i = lastIndex; i >= 0; i --) {
            TableNode tn = candidates.get(i);
            try {
                Table t = tn.eval(new Environment());
                System.out.println("[No." + (i + 1) + "]===============================");
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


        System.out.println("[[Synthesis Status]] " + (candidates.isEmpty()?"Failed":"Succeeded"));
        //System.out.println("[[Synthesis Time]] " + time);
        System.out.printf("[[Synthesis Time]] %.3fs\n", (minute*60. + second + 0.001 * (timeUsed % 1000)));

        return candidates;
    }

    /**
     * Check whether any one of the candidate is a potentially correct candidate based on its filter score
     * @param candidates the set of candidate queries to be checked
     * @return whether a desirable one is contained.
     */
    public static boolean containsDesirableCandidate(List<TableNode> candidates) {
        if (! candidates.isEmpty()) {
            candidates.sort((tn1, tn2) -> Double.compare(tn1.estimateAllFilterCost(), tn2.estimateAllFilterCost()));
            if (candidates.get(0).estimateAllFilterCost() < 10) {
                // go to print the output example only if we have already found a pretty satisfying example.
                return true;
            }
        }
        return false;
    }

    public static Set<NumberVal> guessingPossibleIntegers(EnumConfig config, List<Table> input, Table output) {

        EnumContext ec = new EnumContext(input, config);
        ec.setParameterizedTables(new ArrayList<>());
        ec.setOutputTable(output);

        Set<NumberVal> iSet = new HashSet<>();

        if (config.getAggrFuns().contains(AggregationNode.AggrCount)
                && config.getAggrFuns().contains(AggregationNode.AggrMax))  {
            for (TableNode tn : ec.getTableNodes()) {
                List<RenameTableNode> countResult = AggrEnumerator
                        .enumerateAggregation(Arrays.asList(AggregationNode.AggrCount), tn, true);
                for (TableNode ttn : countResult) {
                    List<RenameTableNode> maxResult = AggrEnumerator
                            .enumerateAggregation(Arrays.asList(AggregationNode.AggrMax), ttn, true);
                    for (TableNode tttn : maxResult) {
                        try {
                            Table t = tttn.eval(new Environment());
                            if (t.getContent().size() == 1 && (t.getContent().get(0).getValue(0) instanceof NumberVal)) {
                                iSet.add(((NumberVal)t.getContent().get(0).getValue(0)));
                            }
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return iSet;
    }
}
