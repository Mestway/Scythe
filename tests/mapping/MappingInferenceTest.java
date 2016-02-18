package mapping;

import enumerator.Constraint;
import enumerator.EnumAggrTableNode;
import enumerator.EnumContext;
import enumerator.TableEnumerator;
import javafx.util.Pair;
import org.junit.Test;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.TableAsVal;
import util.TableInstanceParser;

import java.util.*;

import static org.junit.Assert.*;

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

        MappingInference mi = new MappingInference();
        mi.buildMapping(input, output);
        System.out.println(mi.toString());
        mi.refineMapping();
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
                        AggregationNode.AggrMax, AggregationNode.AggrMin),
                2);
        EnumContext ec = new EnumContext(Arrays.asList(input), c);
        List<TableNode> tns = EnumAggrTableNode.enumAggregationNode(ec);
        tns.add(new NamedTable(input));
        TableNode jn = new JoinNode(tns);
        Table ot = jn.eval(new Environment());
        System.out.println(ot);


        MappingInference mi = new MappingInference();
        mi.buildMapping(ot, output);
        //System.out.println(mi.toString());
        mi.refineMapping();
        //System.out.println("-----");
        System.out.println(mi.toString());
        List<CoordInstMap> instances = mi.genMappingInstances();
        //System.out.println("~~~" + instances.size());
        for (CoordInstMap cim : instances) {
            System.out.println("---");
            //System.out.println(cim.toString());
            Map<Integer, Integer> lineMap = MappingInference.lineMappingInference(cim);
            for (Map.Entry<Integer, Integer> entry : lineMap.entrySet()) {
                System.out.println(entry.getKey() + " - " + entry.getValue());
            }

        }
    }
}