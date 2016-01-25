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
public class EnumTest26 {

    //Basically I need to select the product_ID's that's exclusive to client_id = 2.

    String inputSrc =
            "| Product_ID | Client_ID  |" + "\r\n" +
            "|-------------------------|" + "\r\n" +
            "| 1          | 2          |" + "\r\n" +
            "| 1          | 3          |" + "\r\n" +
            "| 2          | 2          |" + "\r\n" +
            "| 3          | 2          |";


    String outputSrc =
            "| c1 |" + "\r\n" +
            "|----|" + "\r\n" +
            "| 2  |" + "\r\n" +
            "| 3  |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("2")),
            Arrays.asList(AggregationNode.AggrCount),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
