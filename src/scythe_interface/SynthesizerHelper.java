package scythe_interface;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.enum_components.AggrEnumerator;
import forward_enumeration.AbstractTableEnumerator;
import global.GlobalConfig;
import lang.table.Table;
import lang.table.TableRow;
import lang.sql.ast.Environment;
import lang.sql.ast.predicate.BinopPred;
import lang.sql.ast.contable.*;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.ValNode;
import lang.sql.dataval.NumberVal;
import lang.sql.dataval.StringVal;
import lang.sql.dataval.Value;
import lang.sql.exception.SQLEvalException;
import util.Pair;
import util.RenameWrapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 11/1/16.
 */
public class SynthesizerHelper {

    /**
     * The helper function tries to decompose the original output example into multiple small tables
     * and synthesize the program for each small table. The result would be a query formed by union all subqueries.
     * @param inputs input examples
     * @param output the output example
     * @param config configuration that specifies the search depth as well as constants to be used.
     * @param enumerator The enumerator used in query search
     * @param maxDepth the maximum search depth
     * @retur queries
     */
    public static List<TableNode> synthesizeWithDecomposition(List<Table> inputs,
                                                              Table output,
                                                              EnumConfig config,
                                                              AbstractTableEnumerator enumerator,
                                                              int maxDepth) {
        List<TableNode> candidates = new ArrayList<>();
        // try decomposing the output table // vertical decomposition
        for (Pair<Table, Table> decomposed : Table.horizontallyDecompose(output)) {
            if (GlobalConfig.PRINT_LOG)
                System.out.println("  [Try decomposition] \n" + decomposed.getKey().toString() + "\n" + decomposed.getValue().toString());

            config.setMaxDepth(0);
            List<TableNode> left = enumerator.enumProgramWithIO(inputs, decomposed.getKey(), config);
            if (left.isEmpty() && maxDepth == 1) {
                config.setMaxDepth(1);
                left = enumerator.enumProgramWithIO(inputs, decomposed.getKey(), config);
            }
            if (left.isEmpty()) continue;

            config.setMaxDepth(0);
            List<TableNode> right = enumerator.enumProgramWithIO(inputs, decomposed.getValue(), config);
            if (right.isEmpty() && maxDepth == 1) {
                config.setMaxDepth(1);
                right = enumerator.enumProgramWithIO(inputs, decomposed.getValue(), config);
            }
            if (right.isEmpty()) continue;

            left = findTopK(left, 3, config.getUserProvidedConstValues());
            right = findTopK(right, 3, config.getUserProvidedConstValues());

            for (TableNode tn1 : left) {
                for (TableNode tn2 : right) {
                    candidates.add(new UnionNode(Arrays.asList(tn1, tn2)));
                }
            }
            if (candidates.size() > GlobalConfig.MAXIMUM_QUERY_KEPT) {
                candidates.sort(Comparator.comparingDouble(tn -> tn.estimateTotalScore(config.getUserProvidedConstValues())));
                candidates = candidates.subList(0, GlobalConfig.MAXIMUM_QUERY_KEPT);
            }
        }

        for (Pair<Table, Table> decomposed : Table.verticallyDecompose(output)) {
            config.setMaxDepth(0);
            List<TableNode> left = enumerator.enumProgramWithIO(inputs, decomposed.getKey(), config);
            if (left.isEmpty() && maxDepth == 1) {
                config.setMaxDepth(1);
                left = enumerator.enumProgramWithIO(inputs, decomposed.getKey(), config);
            }
            if (left.isEmpty()) continue;

            config.setMaxDepth(0);
            List<TableNode> right = enumerator.enumProgramWithIO(inputs, decomposed.getValue(), config);
            if (right.isEmpty() && maxDepth == 1) {
                config.setMaxDepth(1);
                right = enumerator.enumProgramWithIO(inputs, decomposed.getValue(), config);
            }
            if (right.isEmpty()) continue;

            left = findTopK(left, 3, config.getUserProvidedConstValues());
            right = findTopK(right, 3, config.getUserProvidedConstValues());

            for (TableNode tn1 : left) {
                for (TableNode tn2 : right) {

                    if (tn1 instanceof SelectNode)
                        tn1 = RenameWrapper.tryRename(tn1);
                    if (tn2 instanceof SelectNode)
                        tn2 = RenameWrapper.tryRename(tn2);

                    RenameTableNode jrt = (RenameTableNode) RenameWrapper.tryRename(
                            new JoinNode(Arrays.asList(tn1,
                                    tn2)));

                    List<ValNode> selectFields = new ArrayList<>();
                    int i = 0;
                    for (String s : jrt.getSchema()) {
                        i ++;
                        if (i == 3) continue;
                        selectFields.add(new NamedVal(s));
                    }

                    List<ValNode> compareFields = new ArrayList<>();
                    compareFields.add(new NamedVal(jrt.getSchema().get(0)));
                    compareFields.add(new NamedVal(jrt.getSchema().get(2)));
                    BinopPred filter = new BinopPred(compareFields, BinopPred.eq);

                    candidates.add(new SelectNode(selectFields, jrt, filter));
                }
            }
            if (candidates.size() > GlobalConfig.MAXIMUM_QUERY_KEPT) {
                candidates.sort(Comparator.comparingDouble(tn -> tn.estimateTotalScore(config.getUserProvidedConstValues())));
                candidates = candidates.subList(0, GlobalConfig.MAXIMUM_QUERY_KEPT);
            }
        }

        return candidates;
    }

    // Given the input-output example, guess possible extra constants that may be used in query formulation
    public static Set<NumberVal> guessExtraConstants(List<Function<List<Value>, Value>> aggrFunctions,
                                                     List<Table> input) {

        Set<NumberVal> iSet = new HashSet<>();

        int maxInputTableSize = input.stream().map(i -> i.getContent().size()).reduce(0, (x,y)->(x>y?x:y));

        if (aggrFunctions.contains(AggregationNode.AggrCount)
                && aggrFunctions.contains(AggregationNode.AggrMax))  {

            //This is typically common for max-count / min-count,
            // so we would add some extra constants for the ease of synthesis
            for (TableNode tn : input.stream().map(t -> new NamedTableNode(t)).collect(Collectors.toSet())) {
                List<RenameTableNode> countResult = AggrEnumerator
                        .enumerateAggregation(Arrays.asList(AggregationNode.AggrCount), tn, true);
                for (TableNode ttn : countResult) {
                    List<RenameTableNode> maxResult = AggrEnumerator
                            .enumerateAggregation(Arrays.asList(AggregationNode.AggrMax), ttn, true);
                    for (TableNode mr : maxResult) {
                        try {
                            Table t = mr.eval(new Environment());
                            if (t.getContent().size() == 1 && (t.getContent().get(0).getValue(0) instanceof NumberVal)
                                    && ((NumberVal)t.getContent().get(0).getValue(0)).getVal() <= maxInputTableSize) {
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

    // Given the input-output example, guess functions that will potentially be used in synthesis
    public static List<Set<Function<List<Value>, Value>>> getRelatedFunctions(List<Value> constValues,
                                                                              List<Table> inputs,
                                                                              Table output) {

        List<Set<Function<List<Value>, Value>>> result = new ArrayList<>();

        Set<Value> valuesInInput = new HashSet<>();
        Set<Value> outputAndConstValues = new HashSet<>();
        int maxColumnCnt = inputs.stream().map(t -> t.getContent().size()).reduce(Integer::max).get();

        for (Table t : inputs) {
            for (TableRow tr : t.getContent()) {
                for (Value v :  tr.getValues()) {
                    valuesInInput.add(v);
                }
            }
        }

        for (TableRow tr : output.getContent()) {
            for (Value v :  tr.getValues()) {
                outputAndConstValues.add(v);
            }
        }

        outputAndConstValues.addAll(constValues);

        List<Value> otherValues = new ArrayList<>();
        for (Value v : outputAndConstValues) {
            if (! valuesInInput.contains(v))
                otherValues.add(v);
        }
        if (! otherValues.isEmpty()) {

            // check whether extra string value exists in output but not in input
            boolean containsExtraStringVal = false;
            for (Value v : otherValues) {
                if (v instanceof StringVal) {
                    containsExtraStringVal = true;
                }
            }
            if (containsExtraStringVal) {
                Set<Function<List<Value>, Value>> tmp = new HashSet<>();
                tmp.add(AggregationNode.AggrConcat);
                tmp.add(AggregationNode.AggrConcat2);
                result.add(tmp);
            }

            // check whether small values exists in the output example
            boolean containsSmallVal = false;
            for (Value v : otherValues) {
                if (v instanceof NumberVal && ((double)v.getVal() <= maxColumnCnt))
                    containsSmallVal = true;
            }
            if (containsSmallVal) {
                Set<Function<List<Value>, Value>> tmp = new HashSet<>();
                tmp.add(AggregationNode.AggrCount);
                tmp.add(AggregationNode.AggrCountDistinct);
                result.add(tmp);
            }

            Set<Function<List<Value>, Value>> tmp = new HashSet<>();
            tmp.add(AggregationNode.AggrSum);
            result.add(tmp);
        }

        Set<Function<List<Value>, Value>> tmp = new HashSet<>();
        tmp.add(AggregationNode.AggrMax);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.add(AggregationNode.AggrMin);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.add(AggregationNode.AggrAvg);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.add(AggregationNode.AggrMin);
        tmp.add(AggregationNode.AggrMax);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.add(AggregationNode.AggrCount);
        tmp.add(AggregationNode.AggrMax);
        result.add(tmp);

        return result;
    }

    // Given the input-output example, rank the aggregation functions to try
    public static List<Set<Function<List<Value>, Value>>> rankAggrFunctions(List<Value> constValues, List<Table> input, Table output) {

        List<Set<Function<List<Value>, Value>>> result = getRelatedFunctions(constValues, input, output);

        Set<Function<List<Value>, Value>> tmp = new HashSet<>();
        tmp.add(AggregationNode.AggrCount);
        tmp.add(AggregationNode.AggrSum);
        result.add(tmp);

        tmp = new HashSet<>();
        tmp.addAll(AggregationNode.getAllAggrFunctions());
        tmp.remove(AggregationNode.AggrConcat);
        tmp.remove(AggregationNode.AggrConcat2);
        result.add(tmp);
        return result;
    }

    //Rank the candidates and returns only top k of them
    public static List<TableNode> findTopK(List<TableNode> candidates, int k, List<Value> constValues) {
        if (candidates.isEmpty())
            return candidates;
        else {
            candidates.sort(Comparator.comparingDouble(tn -> tn.estimateTotalScore(constValues)));
            return candidates.subList(0, candidates.size() > k ? k : candidates.size());
        }
    }

}
