package scythe_interface;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.table_enumerator.AbstractTableEnumerator;
import forward_enumeration.table_enumerator.StagedEnumerator;
import global.GlobalConfig;
import global.Statistics;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.*;
import sql.lang.datatype.NumberVal;
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
    public static int maxCandidateKeptPerStage = 5;

    public static List<TableNode> Synthesize(String exampleFilePath, AbstractTableEnumerator enumerator) {

        // read file
        ExampleDS exampleDS = ExampleDS.readFromFile(exampleFilePath);
        if (GlobalConfig.PRINT_LOG) {
            System.out.println("[[Synthesis start]]");
            System.out.println("\tFile: " + exampleFilePath);
            System.out.println("\tEnumerator: " + enumerator.getClass().getSimpleName());
        }

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

            if (GlobalConfig.PRINT_LOG) {
                System.out.println("[[Retry]] Trying to search with depth: " + depth);
                if (depth == 2) System.out.println(output);
            }

            if (depth == 0) {
                //allow using all aggregation functions
                config.setAggrFunctions(AggregationNode.getAllAggrFunctions());
            }

            //##### Synthesis all queries at depth `depth' and rank to obtain top k
            config.setMaxDepth(depth);
            List<TableNode> synthesisResult = enumerator.enumProgramWithIO(inputs, output, config);
            candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                    maxCandidateKeptPerStage, config.getUserProvidedConstValues()));
            config.setAggrFunctions(new ArrayList<>());

            if (depth == 1) {

                synthesisResult = new ArrayList<>();

                // iterate over all candidate aggregation functions,
                // and synthesize with each aggregation function group
                for (Set<Function<List<Value>, Value>> funcSet
                        : SynthesizerHelper.getRelatedFunctions(config.getConstValues(), inputs, output)) {

                    config.setAggrFunctions(funcSet);
                    if (GlobalConfig.PRINT_LOG)
                        System.out.println("    [AggrFun] " + funcSet.stream()
                            .map(f -> AggregationNode.FuncName(f)).reduce(String::concat));

                    EnumConfig tempConfig = config.deepCopy();

                    // guess extra constants
                    Set<NumberVal> extraConstants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
                    if (! extraConstants.isEmpty()) {
                        tempConfig.addConstVals(extraConstants.stream().collect(Collectors.toSet()));
                        tempConfig.containsDerivedConstants = true;
                    }
                    synthesisResult.addAll(enumerator.enumProgramWithIO(inputs, output, tempConfig));

                    config.setAggrFunctions(new ArrayList<>());

                    // note that we won't break here even candidates are found
                    // since some aggregation functions may result in over-fitting
                }

                candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                        maxCandidateKeptPerStage,config.getUserProvidedConstValues()));

                if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;

                //##### Try decompose tables
                if (GlobalConfig.TRY_DECOMPOSITION
                        && output.getContent().size() <= GlobalConfig.TRY_DECOMPOSE_ROW_NUM) {

                    config.setAggrFunctions(
                            Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin)
                    );
                    synthesisResult = SynthesizerHelper
                            .synthesizeWithDecomposition(inputs, output, config, enumerator, 1);

                    if (GlobalConfig.PRINT_LOG)
                        System.out.println(" [Finished Decomposition Synthesis]");

                    candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                            maxCandidateKeptPerStage, config.getUserProvidedConstValues()));
                    config.setMaxDepth(1);
                }
                if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;

                //##### try synthesis with all aggregation functions
                config.setAggrFunctions(AggregationNode.getAllAggrFunctions());
                synthesisResult = enumerator.enumProgramWithIO(inputs, output, config);
                candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                        maxCandidateKeptPerStage, config.getUserProvidedConstValues()));

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
                        Set<NumberVal> constants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
                        config.addConstVals(constants.stream().collect(Collectors.toSet()));
                    }
                    config.setAggrFunctions(funcSet);
                    synthesisResult = enumerator.enumProgramWithIO(inputs, output, config);
                    candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                            maxCandidateKeptPerStage, config.getUserProvidedConstValues()));

                    if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;
                    config.setAggrFunctions(new ArrayList<>());
                }

                //**** try synthesizing queries with Exists-clauses
                for (Table existsCore : inputs) {
                    config.setExistsCore(2, Arrays.asList(existsCore));
                    config.setMaxDepth(0);

                    synthesisResult = enumerator.enumProgramWithIO(inputs, output, config);
                    candidates.addAll(SynthesizerHelper.findTopK(synthesisResult,
                            maxCandidateKeptPerStage, config.getUserProvidedConstValues()));

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

        if (GlobalConfig.STAT_MODE) {
            if (enumerator instanceof StagedEnumerator) {
                System.out.println("[AbstractSearchPrunedCount] "
                        + ((double)Statistics.PrunedAbstractTableCount)/((double)Statistics.TotalTableTryEvaluated));
            }
        }
        for (int i = 0; i < topCandidates.size(); i ++) {
            TableNode tn = candidates.get(i);
            try {
                Table t = tn.eval(new Environment());
                System.out.println("[Query No." + (i + 1) + "]===============================");
                if (GlobalConfig.PRINT_LOG)
                    System.out.println(tn.printQuery());
                //System.out.println(t);
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }

        // formatting time
        timeUsed = System.currentTimeMillis() - timeStart;
        long second = (timeUsed / 1000) % 60;
        long minute = (timeUsed / (1000 * 60)) % 60;

        if (GlobalConfig.PRINT_LOG) {
            System.out.println("[[Synthesis Status]] " + (candidates.isEmpty()?"Failed":"Succeeded"));
            System.out.printf("[[Synthesis Time]] %.3fs\n", (minute*60. + second + 0.001 * (timeUsed % 1000)));
        }

        return candidates;
    }

    public static List<TableNode> SynthesizeWAggr(String exampleFilePath, AbstractTableEnumerator enumerator) {

        // read file
        ExampleDS exampleDS = ExampleDS.readFromFile(exampleFilePath);

        if (GlobalConfig.PRINT_LOG) {
            System.out.println("[[Synthesis start]]");
            System.out.println("\tFile: " + exampleFilePath);
            System.out.println("\tEnumerator: " + enumerator.getClass().getSimpleName());
        }

        long timeUsed = 0;
        long timeStart = System.currentTimeMillis();

        EnumConfig config = exampleDS.enumConfig;
        List<Table> inputs = exampleDS.inputs;
        Table output = exampleDS.output;

        List<TableNode> candidates = new ArrayList<>();

        if (GlobalConfig.GUESS_ADDITIONAL_CONSTANTS) {
            // guess constants
            Set<NumberVal> guessedNumConstants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
            config.addConstVals(guessedNumConstants.stream().collect(Collectors.toSet()));
        }

        int depth = 0;
        while (timeUsed < Synthesizer.TimeOut) {

            if (GlobalConfig.PRINT_LOG) {
                System.out.println("[[Retry]] Depth: " + depth);
                if (depth == 2)
                    System.out.println(output);
            }

            //##### Synthesis
            config.setMaxDepth(depth);
            candidates.addAll(enumerator.enumProgramWithIO(inputs, output, config));
            if (depth > 0 && containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;

            List<Pair<Table, Table>> lp = Table.horizontalDecompose(output);

            if (depth == 1) {
                // try decompose tables
                if (GlobalConfig.TRY_DECOMPOSITION
                        && exampleDS.output.getContent().size() <= GlobalConfig.TRY_DECOMPOSE_ROW_NUM) {
                    candidates.addAll(SynthesizerHelper
                            .synthesizeWithDecomposition(inputs, output, config, enumerator,1));
                    config.setMaxDepth(1);
                }
                if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;
            }

            // backtrack on aggregation functions
            //exampleDS.enumConfig.setAggrFunctions(new ArrayList<>());

            if (depth == 2) {
                // try synthesizing queries with Exists-clauses
                for (Table existsCore : exampleDS.inputs) {
                    config.setExistsCore(2, Arrays.asList(existsCore));
                    config.setMaxDepth(0);

                    candidates.addAll(enumerator
                            .enumProgramWithIO(exampleDS.inputs, exampleDS.output, exampleDS.enumConfig));
                    if (containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;
                }
            }

            exampleDS.enumConfig.setExistsCore(0, new ArrayList<>());
            exampleDS.enumConfig.setMaxDepth(depth);

            timeUsed = System.currentTimeMillis() - timeStart;
            depth ++;

            if (depth > 1 && containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;
        }

        candidates.sort((tn1, tn2) -> Double.compare(tn1.estimateTotalScore(config.getUserProvidedConstValues()),
                tn2.estimateTotalScore(config.getUserProvidedConstValues())));
        int lastIndex = GlobalConfig.MAXIMUM_QUERY_KEPT > candidates.size() ? candidates.size() - 1
                : GlobalConfig.MAXIMUM_QUERY_KEPT - 1;
        for (int i = lastIndex; i >= 0; i --) {
            TableNode tn = candidates.get(i);
            try {
                Table t = tn.eval(new Environment());
                System.out.println("[No." + (i + 1) + "]===============================");
                System.out.println(tn.printQuery());
                if (GlobalConfig.PRINT_LOG)
                    System.out.println(t);
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }

        if (GlobalConfig.STAT_MODE) {
            if (enumerator instanceof StagedEnumerator) {
                System.out.println("[AbstractSearchPrunedCount] "
                        + ((double) Statistics.PrunedAbstractTableCount)/((double)Statistics.TotalTableTryEvaluated));
            }
        }

        // formatting time
        timeUsed = System.currentTimeMillis() - timeStart;
        long second = (timeUsed / 1000) % 60;
        long minute = (timeUsed / (1000 * 60)) % 60;

        if (GlobalConfig.PRINT_LOG) {
            System.out.println("[[Synthesis Status]] " + (candidates.isEmpty() ? "Failed" : "Succeeded"));
            System.out.printf("[[Synthesis Time]] %.3fs\n", (minute * 60. + second + 0.001 * (timeUsed % 1000)));
        }
        return candidates;
    }

    /**
     * Check whether any one of the candidate is a potentially correct candidate based on its filter score
     * @param candidates the set of candidate queries to be checked
     * @return whether a desirable one is contained.
     */
    public static boolean containsDesirableCandidate(List<TableNode> candidates, List<Value> constants) {
        if (! candidates.isEmpty()) {
            candidates.sort(Comparator.comparingDouble(tn -> tn.estimateTotalScore(constants)));
            if (candidates.get(0).estimateAllFilterCost() <= GlobalConfig.DESIRABLE_CANDIDATE_QUERY_SCORE) {
                // go to print the output example only if we have already found a pretty satisfying example.
                return true;
            }
        }
        return false;
    }

}
