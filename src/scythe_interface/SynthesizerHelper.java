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
 * Created by clwang on 11/1/16.
 */
public class SynthesizerHelper {

    public static List<TableNode> synthesizeWithDecomposition(List<Table> inputs,
                                                              Table output,
                                                              EnumConfig config,
                                                              AbstractTableEnumerator enumerator,
                                                              int maxDepth) {
        List<TableNode> candidates = new ArrayList<>();
        // try decomposing the output table
        for (Pair<Table, Table> decomposed : Table.tryDecompose(output)) {
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
                candidates.sort((tn1, tn2)
                        -> Double.compare(tn1.estimateTotalScore(config.getUserProvidedConstValues()),
                                          tn2.estimateTotalScore(config.getUserProvidedConstValues())));
                candidates = candidates.subList(0, GlobalConfig.MAXIMUM_QUERY_KEPT);
            }
        }
        return candidates;
    }

    // Given the input-output example, guess
    public static Set<NumberVal> guessExtraConstants(List<Function<List<Value>, Value>> aggrFunctions,
                                                     List<Table> input) {

        Set<NumberVal> iSet = new HashSet<>();

        int maxInputTableSize = input.stream().map(i -> i.getContent().size()).reduce(0, (x,y)->(x>y?x:y));

        if (aggrFunctions.contains(AggregationNode.AggrCount)
                && aggrFunctions.contains(AggregationNode.AggrMax))  {
            for (TableNode tn : input.stream().map(t -> new NamedTable(t)).collect(Collectors.toSet())) {
                List<RenameTableNode> countResult = AggrEnumerator
                        .enumerateAggregation(Arrays.asList(AggregationNode.AggrCount), tn, true);
                for (TableNode ttn : countResult) {
                    List<RenameTableNode> maxResult = AggrEnumerator
                            .enumerateAggregation(Arrays.asList(AggregationNode.AggrMax), ttn, true);
                    for (TableNode tttn : maxResult) {
                        try {
                            Table t = tttn.eval(new Environment());
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

    // Given the input-output example, guess
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
            boolean containsExtraStringVal = false;
            for (Value v : otherValues) {
                if (v instanceof StringVal) {
                    containsExtraStringVal = true;
                    System.out.println(v);
                }
            }
            if (containsExtraStringVal) {
                Set<Function<List<Value>, Value>> tmp = new HashSet<>();
                tmp.add(AggregationNode.AggrConcat);
                tmp.add(AggregationNode.AggrConcat2);
                result.add(tmp);
            }
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
            candidates.sort((tn1, tn2) -> Double.compare(tn1.estimateTotalScore(constValues), tn2.estimateTotalScore(constValues)));
            return candidates.subList(0, candidates.size() > k ? k : candidates.size());
        }
    }

}
