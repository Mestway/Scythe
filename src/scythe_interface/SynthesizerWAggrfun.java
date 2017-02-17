package scythe_interface;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.table_enumerator.AbstractTableEnumerator;
import forward_enumeration.table_enumerator.StagedEnumerator;
import global.GlobalConfig;
import global.Statistics;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.table.UnionNode;
import sql.lang.datatype.NumberVal;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/22/16.
 */
public class SynthesizerWAggrfun {

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

        List<TableNode> candidates = new ArrayList<>();

        // guess constants
        Set<NumberVal> guessedNumConstants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
        config.addConstVals(guessedNumConstants.stream().collect(Collectors.toSet()));

        int depth = 0;
        while (timeUsed < SynthesizerWAggrfun.TimeOut) {
            System.out.println("[[Retry]] Depth: " + depth);

            if (depth == 2)
                System.out.println(output);

            //##### Synthesis
            config.setMaxDepth(depth);
            candidates.addAll(enumerator.enumProgramWithIO(inputs, output, config));
            if (depth > 0 && containsDesirableCandidate(candidates, config.getUserProvidedConstValues())) break;

            if (depth == 1) {
                // try decompose tables
                if (GlobalConfig.TRY_DECOMPOSITION
                        && exampleDS.output.getContent().size() <= GlobalConfig.TRY_DECOMPOSE_ROW_NUM) {
                    candidates.addAll(SynthesizerHelper.synthesizeWithDecomposition(inputs, output, config, enumerator,1));
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

                    candidates.addAll(enumerator.enumProgramWithIO(exampleDS.inputs, exampleDS.output, exampleDS.enumConfig));
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
                System.out.println(tn.prettyPrint(0));
                System.out.println("\n");
                System.out.println(t);
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }

        if (GlobalConfig.STAT_MODE) {
            if (enumerator instanceof StagedEnumerator) {
                System.out.println("[AbstractSearchPrunedCount] " + ((double) Statistics.PrunedAbstractTableCount)/((double)Statistics.TotalTableTryEvaluated));
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
    public static boolean containsDesirableCandidate(List<TableNode> candidates, List<Value> constValues) {
        if (! candidates.isEmpty()) {
            candidates.sort((tn1, tn2) -> Double.compare(tn1.estimateTotalScore(constValues),
                    tn2.estimateTotalScore(constValues)));
            if (candidates.get(0).estimateTotalScore(constValues) < 10) {
                // go to print the output example only if we have already found a pretty satisfying example.
                return true;
            }
        }
        return false;
    }

}
