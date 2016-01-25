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
public class EnumTest38 {

    //I want to display the latest inserted records in gridview.


    String inputSrc =
            "|  Id | Alerts  |  Alert_Date          |" + "\r\n" +
            "|--------------------------------------|" + "\r\n" +
            "|  1  | Alert1  |  05/11/2015 12:12:22 |" + "\r\n" +
            "|  2  | Alert2  |  05/11/2015 12:12:22 |" + "\r\n" +
            "|  3  | Alert1  |  05/12/2015 12:12:22 |" + "\r\n" +
            "|  4  | Alert2  |  05/13/2015 12:12:22 |" + "\r\n" +
            "|  5  | Alert2  |  05/14/2015 12:12:22 |" + "\r\n" +
            "|  6  | Alert3  |  05/14/2015 12:12:22 |";


    String outputSrc =
            "| Alerts | Alert_Date          |" + "\r\n" +
            "|------------------------------|" + "\r\n" +
            "| Alert1 | 05/12/2015 12:12:22 |" + "\r\n" +
            "| Alert2 | 05/14/2015 12:12:22 |" + "\r\n" +
            "| Alert3 | 05/14/2015 12:12:22 |";

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
