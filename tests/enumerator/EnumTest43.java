package enumerator;

import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/8/16.
 */
public class EnumTest43 {

    //How can I select one row per productId with minimum orderIndex that not rejected?

    String inputSrc =
            "| id | productId | orderIndex | rejected |" + "\r\n" +
            "|----------------------------------------|" + "\r\n" +
            "| 1  |  1        |   0        |   1      |" + "\r\n" +
            "| 2  |  1        |   1        |   0      |" + "\r\n" +
            "| 3  |  1        |   2        |   0      |" + "\r\n" +
            "| 4  |  2        |   0        |   0      |" + "\r\n" +
            "| 5  |  2        |   1        |   1      |" + "\r\n" +
            "| 6  |  3        |   0        |   0      |";


    String outputSrc =
            "| id | productId | orderIndex | rejected |" + "\r\n" +
            "|----------------------------------------|" + "\r\n" +
            "| 2  |  1        |   1        |   0      |" + "\r\n" +
            "| 4  |  2        |   0        |   0      |" + "\r\n" +
            "| 6  |  3        |   0        |   0      |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMax),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
