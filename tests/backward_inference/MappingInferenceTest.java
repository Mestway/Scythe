package backward_inference;

import forward_enumeration.Constraint;
import forward_enumeration.context.EnumContext;
import forward_enumeration.table_enumerator.hueristics.TableNaturalJoinWithAggr;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.*;
import util.TableInstanceParser;

import java.util.*;

/**
 * Created by clwang on 2/17/16.
 */
public class MappingInferenceTest {


    @Test
    public void testBuildMapping0() throws Exception {
        String inputSrc =
                "| T1.User   |  T1.Phone   |  T1.Value  | T2.User | T2.MaxVal |" + "\r\n" +
                        "|---------------------------------------------|" + "\r\n" +
                        "| Peter    |  0       |  1        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  0       |  1        | Paul  | 7 |" + "\r\n" +
                        "| Peter    |  456     |  2        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  456     |  2        | Paul  | 7 |" + "\r\n" +
                        "| Peter    |  456     |  3        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  456     |  3        | Paul  | 7 |" + "\r\n" +
                        "| Paul     |  456     |  7        | Peter | 3 |" + "\r\n" +
                        "| Paul     |  456     |  7        | Paul  | 7 |" + "\r\n" +
                        "| Paul     |  789     |  10        | Peter | 3 |" + "\r\n" +
                        "| Paul     |  789     |  10        | Paul  | 7 |";

        String outputSrc =
                "| col1 | col2 | col3 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  Peter   |  456  |  3   |" + "\r\n" +
                        "|  Paul    |  456  |  7   |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        MappingInference mi = MappingInference.buildMapping(input, output);
        System.out.println("-----");
        System.out.println(mi.toString());
        List<CoordInstMap> instances = mi.genMappingInstancesWColumnBarrier(Arrays.asList(3, 3 + 2));
        for (CoordInstMap cim : instances) {
           // System.out.println("---");
           // System.out.println(cim.toString());
        }

        System.out.println(instances.size());

        MappingInference.printColumnMapping(mi.genColumnMappingInstances());
        MappingInference.printColumnMapping(mi.genRowMappingInstances());

        List<CoordInstMap> is = mi.genMappingInstances();
        System.out.println(is.size());
        for (CoordInstMap cim : is) {
            //System.out.println("---");
            //System.out.println(cim.toString());
        }

        System.out.println(is.size());

    }

    public void testBuildMapping() throws Exception {
        String inputSrc =
            "| id   |  rev   |  content  | n | m |" + "\r\n" +
            "|-----------------------------------|" + "\r\n" +
            "| 1    |  1     |  A        | 1 | a |" + "\r\n" +
            "| 2    |  1     |  B        | 1 | a |" + "\r\n" +
            "| 1    |  2     |  C        | 1 | a |" + "\r\n" +
            "| 1    |  3     |  D        | 1 | a |" + "\r\n" +
            "| 1    |  1     |  A        | 2 | b |" + "\r\n" +
            "| 2    |  1     |  B        | 2 | b |" + "\r\n" +
            "| 1    |  2     |  C        | 2 | b |" + "\r\n" +
            "| 1    |  3     |  D        | 2 | b |" + "\r\n" +
            "| 1    |  1     |  A        | 1 | c |" + "\r\n" +
            "| 2    |  1     |  B        | 1 | c |" + "\r\n" +
            "| 1    |  2     |  C        | 1 | c |" + "\r\n" +
            "| 1    |  3     |  D        | 1 | c |";

        String outputSrc =
            "| col1 | col2 | col3 |" + "\r\n" +
            "|--------------------|" + "\r\n" +
            "|  1   |  3   |  D   |" + "\r\n" +
            "|  2   |  1   |  B   |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        MappingInference mi = MappingInference.buildMapping(input, output);
        System.out.println("-----");
        System.out.println(mi.toString());
        List<CoordInstMap> instances = mi.genMappingInstancesWColumnBarrier(Arrays.asList(3, 3 + 2));
        for (CoordInstMap cim : instances) {
            System.out.println("---");
            System.out.println(cim.toString());
        }

        MappingInference.printColumnMapping(mi.genColumnMappingInstances());
        MappingInference.printColumnMapping(mi.genRowMappingInstances());


    }

    public void testMapping() throws Exception {
        String inputSrc =
                "| id   |  rev   |  content  |" + "\r\n" +
                "|----------------------------------|" + "\r\n" +
                "| 1    |  1     |  A        |" + "\r\n" +
                "| 2    |  1     |  B        |" + "\r\n" +
                "| 1    |  2     |  C        |" + "\r\n" +
                "| 1    |  3     |  D        |";
        String outputSrc =
                "| col1 | col2 | col3 |" + "\r\n" +
                "|--------------------|" + "\r\n" +
                "|  1   |  3   |  D   |" + "\r\n" +
                "|  2   |  1   |  B   |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        Constraint c = new Constraint(
                2,
                new ArrayList<>(),
                Arrays.asList(
                        AggregationNode.AggrMax, AggregationNode.AggrCount),
                2);
        EnumContext ec = new EnumContext(Arrays.asList(input), c);


        // Using only equi-join
        /*
        TableNode natExt = TableNaturalJoinWithAggr.naturalTableExtensionWithAggr(ec);
        System.out.println(natExt.eval(new Environment()));
        System.out.println();
        */

        // traditional approach:
        /*
        List<TableNode> tns = EnumAggrTableNode.enumAggregationNode(ec);
        tns.add(new NamedTable(input));
        TableNode jn = new JoinNode(tns);
        Table ot = jn.eval(new Environment());
        System.out.println(ot);
        */

        Table ot = TableNaturalJoinWithAggr.naturalTableExtensionWithAggr(ec).eval(new Environment());

        System.out.println(ot.toString());

        MappingInference mi = MappingInference.buildMapping(ot, output);
        System.out.println("[Refine Mapping Done]");
        System.out.println(mi.toString());

        List<CoordInstMap> instances = mi.genMappingInstances();
        System.out.println("[Gen MappingInstance Done] size: " + instances.size());
        for (CoordInstMap cim : instances) {
            System.out.println("---");
            System.out.println(cim.toString());
            Map<Integer, Integer> lineMap = MappingInference.columnMappingInference(cim);
            for (Map.Entry<Integer, Integer> entry : lineMap.entrySet()) {
                //System.out.println(entry.getKey() + " - " + entry.getValue());
            }
        }
    }
}