package scythe_interface;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.primitive.AggrEnumerator;
import forward_enumeration.table_enumerator.AbstractTableEnumerator;
import global.GlobalConfig;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.table.*;
import sql.lang.ast.val.ConstantVal;
import sql.lang.datatype.NumberVal;
import sql.lang.datatype.StringVal;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import util.Pair;

import java.util.*;
import java.util.function.Function;
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

        EnumConfig config = exampleDS.enumConfig;
        List<Table> inputs = exampleDS.inputs;
        Table output = exampleDS.output;

        // Ignore provided aggregation functions
        config.setAggrFunctions(new ArrayList<>());

        List<TableNode> candidates = new ArrayList<>();

        int maxDepth = 0;
        while (timeUsed < Synthesizer.TimeOut) {
            System.out.println("[[Retry]] Maximum Depth: " + maxDepth);

            if (maxDepth == 2) System.out.println(output);

            if (maxDepth == 0) {
                config.setAggrFunctions(AggregationNode.getAllAggrFunctions());
            }
            //##### Synthesis
            config.setMaxDepth(maxDepth);
            candidates.addAll(enumerator.enumProgramWithIO(inputs, output, config));
            config.setAggrFunctions(new ArrayList<>());

            if (maxDepth == 1) {

                List<Set<Function<List<Value>, Value>>> aggrFunctionSets =
                        SynthesizerHelper.getRelatedFunctions(config.getConstValues(), inputs, output);
                for (Set<Function<List<Value>, Value>> funcSet : aggrFunctionSets) {

                    config.setAggrFunctions(funcSet);

                    EnumConfig tempConfig = config.deepCopy();
                    if (!containsDesirableCandidate(candidates)) {
                        // guess constants
                        Set<NumberVal> guessedNumConstants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
                        tempConfig.addConstVals(guessedNumConstants.stream().collect(Collectors.toSet()));
                    }
                    candidates.addAll(enumerator.enumProgramWithIO(inputs, output, tempConfig));

                    //if (containsDesirableCandidate(candidates)) break;
                    config.setAggrFunctions(new ArrayList<>());
                }
                if (containsDesirableCandidate(candidates)) break;

                //##### Try decompose tables
                if (GlobalConfig.TRY_DECOMPOSITION
                        && output.getContent().size() <= GlobalConfig.TRY_DECOMPOSE_ROW_NUM) {
                    config.setMaxDepth(1);
                    config.setAggrFunctions(SynthesizerHelper.getRelatedFunctions(config.getConstValues(), inputs, output).get(0));
                    candidates.addAll(SynthesizerHelper.synthesizeWithDecomposition(inputs, output, config, enumerator));
                    config.setMaxDepth(1);
                }
                if (containsDesirableCandidate(candidates)) break;

                //##### try synthesis with aggregation functions
                config.setAggrFunctions(AggregationNode.getAllAggrFunctions());
                candidates.addAll(enumerator.enumProgramWithIO(inputs, output, config));

                if (containsDesirableCandidate(candidates)) break;
                config.setAggrFunctions(new ArrayList<>());
            }

            if (maxDepth == 2) {

                if (containsDesirableCandidate(candidates)) break;

                List<Set<Function<List<Value>, Value>>> aggreFunctions =
                        SynthesizerHelper.rankAggrFunctions(config.getConstValues(), inputs, output);

                for (Set<Function<List<Value>, Value>> funcSet : aggreFunctions) {
                    // guess constants
                    if (!containsDesirableCandidate(candidates)) {
                        Set<NumberVal> guessedNumConstants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
                        config.addConstVals(guessedNumConstants.stream().collect(Collectors.toSet()));
                    }
                    config.setAggrFunctions(funcSet);
                    candidates.addAll(enumerator.enumProgramWithIO(inputs, output, config));
                    if (containsDesirableCandidate(candidates)) break;
                    config.setAggrFunctions(new ArrayList<>());
                }
            }

            if (maxDepth == 2) {
                // try synthesizing queries with Exists-clauses
                for (Table existsCore : inputs) {
                    config.setExistsCore(2, Arrays.asList(existsCore));
                    config.setMaxDepth(0);

                    candidates.addAll(enumerator.enumProgramWithIO(inputs, output, config));
                    if (containsDesirableCandidate(candidates)) break;
                }
                config.setExistsCore(0, new ArrayList<>());
            }

            config.setMaxDepth(maxDepth);

            timeUsed = System.currentTimeMillis() - timeStart;
            maxDepth ++;

            if (maxDepth > 1 && containsDesirableCandidate(candidates)) break;
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



}
