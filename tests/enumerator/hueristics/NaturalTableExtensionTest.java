package enumerator.hueristics;

import enumerator.Constraint;
import enumerator.EnumContext;
import jdk.nashorn.internal.runtime.Debug;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by clwang on 1/8/16.
 */
public class NaturalTableExtensionTest {

    @Test
    public void testNaturalJoinWithAggregation() throws Exception {
        String inputSrc =
                "| id   |  rev   |  content  |" + "\r\n" +
                        "|---------------------------|" + "\r\n" +
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
        Table output = TableInstanceParser.parseMarkDownTable("output", outputSrc);

        Constraint c = new Constraint(3, new ArrayList<>(), Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin));

        DebugHelper.printTableNodes(NaturalTableExtension.naturalJoinWithAggregation(new EnumContext(Arrays.asList(input), c)));
    }
}