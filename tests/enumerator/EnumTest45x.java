package enumerator;

import org.junit.Test;
import sql.lang.DataType.Value;
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
public class EnumTest45x {

    //The aggregation function for group by only allow me to get the highest score for each name.
    // I would like to make a query to get the highest 2 score for each name, how should I do?


    String inputSrc =
            "| NAME  | SCORE |" + "\r\n" +
            "|---------------|" + "\r\n" +
            "| willy |     1 |" + "\r\n" +
            "| willy |     2 |" + "\r\n" +
            "| willy |     3 |" + "\r\n" +
            "| zoe   |     4 |" + "\r\n" +
            "| zoe   |     5 |" + "\r\n" +
            "| zoe   |     6 |";


    String outputSrc =
            "| NAME  | SCORE |" + "\r\n" +
            "|---------------|" + "\r\n" +
            "| willy |     2 |" + "\r\n" +
            "| willy |     3 |" + "\r\n" +
            "| zoe   |     5 |" + "\r\n" +
            "| zoe   |     6 |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("2")),
            Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrCount),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
