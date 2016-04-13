package enumerator;

import enumerator.context.EnumContext;
import enumerator.primitive.tables.EnumAggrTableNode;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 2/8/16.
 */
public class EnumAggrTableNodeTest {

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
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);
    Constraint c = new Constraint(
            2,
            new ArrayList<>(),
            Arrays.asList(
                    AggregationNode.AggrMax),
            2);

    @Test
    public void testEnumAggregationNode() throws Exception {
        EnumContext ec = new EnumContext(Arrays.asList(input), c);
        List<TableNode> tns = EnumAggrTableNode.enumAggregationNode(ec);
        DebugHelper.printTableNodes(tns.stream().map(tn -> (TableNode) tn).collect(Collectors.toList()));
    }
}