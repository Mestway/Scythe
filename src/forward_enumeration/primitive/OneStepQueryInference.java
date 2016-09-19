package forward_enumeration.primitive;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.tables.EnumAggrTableNode;
import forward_enumeration.primitive.tables.EnumFilterNamed;
import forward_enumeration.primitive.tables.EnumJoinTableNodes;
import forward_enumeration.primitive.tables.EnumProjection;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 4/7/16.
 */
public class OneStepQueryInference {

    // Inference aggregation from one table.
    public static List<TableNode> inferFromAggr(TableNode inputTableNode, Table output, EnumContext ec) {
        ec.setTableNodes(Arrays.asList(inputTableNode));
        return EnumAggrTableNode.enumAggregationNodeFlag(ec, true, false).stream()
                .filter(tn -> {
                    try {
                        return tn.eval(new Environment()).equals(output);
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                    return false;
                }).collect(Collectors.toList());
    }


    public static List<TableNode> infer(List<TableNode> inputTableNodes, Table output, EnumContext ec) {
        if (inputTableNodes.size() == 1) {
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
