package mapping;

import enumerator.Constraint;
import enumerator.context.EnumContext;
import enumerator.tableenumerator.hueristics.TableNaturalJoinWithAggr;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
import util.TableInstanceParser;

import java.util.*;

/**
 * Created by clwang on 2/17/16.
 */
public class MappingInferenceTest {

    public void testBuildMapping() throws Exception {
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

        MappingInference mi = MappingInference.buildMapping(input, output);
        System.out.println("-----");
        System.out.println(mi.toString());
        List<CoordInstMap> instances = mi.genMappingInstances();
        for (CoordInstMap cim : instances) {
            System.out.println("---");
            System.out.println(cim.toString());
        }
    }

    @Test
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

        List<Filter> atomicFilters = mi.atomicFilterEnum(new ArrayList<>(), 2);

        System.out.println("[Atomic Filter Enum Done] size: " + atomicFilters.size());
        MappingInference.filterMemoization(ot, atomicFilters);
    }
}