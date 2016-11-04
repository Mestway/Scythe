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
    public static int maxCandidateKeptEachStage = 5;

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

        int depth = 0;
        while (timeUsed < Synthesizer.TimeOut) {
            System.out.println("[[Retry]] Trying to search for depth: " + depth);

            if (depth == 2) System.out.println(output);

            if (depth == 0) {
                //allow using all aggregation functions
                config.setAggrFunctions(AggregationNode.getAllAggrFunctions());
            }

            //##### Synthesis
            config.setMaxDepth(depth);
            List<TableNode> synthesisResult = enumerator.enumProgramWithIO(inputs, output, config);
            candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                                                         maxCandidateKeptEachStage * 2,
                                                         config.getUserProvidedConstValues()));
            config.setAggrFunctions(new ArrayList<>());

            if (depth == 1) {

                synthesisResult = new ArrayList<>();
                // iterate over all candidate aggregation functions
                for (Set<Function<List<Value>, Value>> funcSet
                        : SynthesizerHelper.getRelatedFunctions(config.getConstValues(), inputs, output)) {

                    config.setAggrFunctions(funcSet);

                    EnumConfig tempConfig = config.deepCopy();
                    if (!containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) {
                        // guess constants
                        Set<NumberVal> guessedNumConstants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
                        tempConfig.addConstVals(guessedNumConstants.stream().collect(Collectors.toSet()));
                        tempConfig.containsDerivedConstants = true;
                    }

                    synthesisResult.addAll(enumerator.enumProgramWithIO(inputs, output, tempConfig));

                    //if (containsDesirableCandidate(candidates)) break;
                    config.setAggrFunctions(new ArrayList<>());
                }
                candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                                                             maxCandidateKeptEachStage,
                                                             config.getUserProvidedConstValues()));
                if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;

                //##### Try decompose tables
                if (GlobalConfig.TRY_DECOMPOSITION
                        && output.getContent().size() <= GlobalConfig.TRY_DECOMPOSE_ROW_NUM) {

                    config.setAggrFunctions(
                            Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin)
                    );
                    synthesisResult = SynthesizerHelper.synthesizeWithDecomposition(inputs, output, config, enumerator, 1);

                    System.out.println(" [Finished Decomposition Synthesis]");
                    candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                            maxCandidateKeptEachStage, config.getUserProvidedConstValues()));
                    config.setMaxDepth(1);
                }
                if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;

                //##### try synthesis with all aggregation functions
                config.setAggrFunctions(AggregationNode.getAllAggrFunctions());
                synthesisResult = enumerator.enumProgramWithIO(inputs, output, config);
                candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                        maxCandidateKeptEachStage, config.getUserProvidedConstValues()));

                if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;
                config.setAggrFunctions(new ArrayList<>());
            }

            if (depth == 2) {

                if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;

                List<Set<Function<List<Value>, Value>>> aggreFunctions =
                        SynthesizerHelper.rankAggrFunctions(config.getConstValues(), inputs, output);

                for (Set<Function<List<Value>, Value>> funcSet : aggreFunctions) {
                    // guess constants
                    if (!containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) {
                        Set<NumberVal> guessedNumConstants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
                        config.addConstVals(guessedNumConstants.stream().collect(Collectors.toSet()));
                    }
                    config.setAggrFunctions(funcSet);
                    synthesisResult = enumerator.enumProgramWithIO(inputs, output, config);
                    candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                                                maxCandidateKeptEachStage, config.getUserProvidedConstValues()));

                    if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;
                    config.setAggrFunctions(new ArrayList<>());
                }

                //**** try synthesizing queries with Exists-clauses
                for (Table existsCore : inputs) {
                    config.setExistsCore(2, Arrays.asList(existsCore));
                    config.setMaxDepth(0);

                    synthesisResult = enumerator.enumProgramWithIO(inputs, output, config);
                    candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                                                maxCandidateKeptEachStage, config.getUserProvidedConstValues()));

                    if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;
                }
                config.setExistsCore(0, new ArrayList<>());
            }

            config.setMaxDepth(depth);

            timeUsed = System.currentTimeMillis() - timeStart;
            depth ++;

            if (depth > 1 && containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;
        }

        List<TableNode> topCandidates = SynthesizerHelper.findTopK(candidates,
                GlobalConfig.MAXIMUM_QUERY_KEPT, config.getUserProvidedConstValues());

        for (int i = topCandidates.size() - 1; i >= 0; i --) {
            TableNode tn = candidates.get(i);
            try {
                Table t = tn.eval(new Environment());
                System.out.println("[Query No." + (i + 1) + "]===============================");
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
    public static boolean containsDesirableCandidate(List<TableNode> candidates, List<Value> constants) {
        if (! candidates.isEmpty()) {
            candidates.sort((tn1, tn2) -> Double.compare(tn1.estimateTotalScore(constants), tn2.estimateTotalScore(constants)));
            if (candidates.get(0).estimateAllFilterCost() < 10) {
                // go to print the output example only if we have already found a pretty satisfying example.
                return true;
            }
        }
        return false;
    }

}
