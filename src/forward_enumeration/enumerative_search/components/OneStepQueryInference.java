package forward_enumeration.enumerative_search.components;

import forward_enumeration.context.EnumContext;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 4/7/16.
 * this class help up identify this one step query,
 *   when we have already known that we can obtain TableB from TableA (or a set of tables TableA1,TableA2...) in one step,
 */
public class OneStepQueryInference {

    public static List<TableNode> infer(List<TableNode> inputTableNodes, Table output, EnumContext ec) {

        if (inputTableNodes.size() == 1) {

            // if input table nodes contains only one table,
            // then it can either be obtained from select(filter), aggregation or projection

            List<TableNode> tns = new ArrayList<>();
            ec.setTableNodes(inputTableNodes);

            tns.addAll(EnumAggrTableNode.enumAggregationNode(ec).stream()
                    .filter(tn -> {
                        try {
                            return tn.eval(new Environment()).equals(output);
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }).collect(Collectors.toList()));

            tns.addAll(EnumFilterNamed.enumFilterNamed(ec).stream().filter(tn -> {
                try {
                    return (tn.eval(new Environment()).equals(output));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));

            tns.addAll(EnumProjection.enumProjection(ec, output));

            tns.addAll(EnumJoinTableNodes.enumJoinWithFilter(ec).stream().filter(tn -> {
                try {
                    return (tn.eval(new Environment()).equals(output));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));

            return tns;
        } else if (inputTableNodes.size() == 2) {

            // if the output table is generated from 2 input tables, the only way is by join (for now)

            List<TableNode> tns = new ArrayList<>();
            ec.setTableNodes(inputTableNodes);
            tns.addAll(EnumJoinTableNodes.enumJoinWithFilter(ec).stream().filter(t -> {
                try {
                    return t.eval(new Environment()).equals(output);
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));
            return tns;
        }
        return new ArrayList<>();
    }
}
