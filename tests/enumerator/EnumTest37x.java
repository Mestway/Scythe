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
public class EnumTest37x {

    //I have following table and I want to display all names along with the user_id where unique user_id count>3.


    String inputSrc =
            "| user_id |  names |" + "\r\n" +
            "|------------------|" + "\r\n" +
            "| 701     |  Name1 |" + "\r\n" +
            "| 701     |  Name2 |" + "\r\n" +
            "| 701     |  Name3 |" + "\r\n" +
            "| 701     |  Name4 |" + "\r\n" +
            "| 702     |  Name5 |" + "\r\n" +
            "| 702     |  Name6 |" + "\r\n" +
            "| 703     |  Name7 |" + "\r\n" +
            "| 703     |  Name8 |";

    String inputSrc2 =
            "| c | user_id |  names |" + "\r\n" +
            "|------------------|" + "\r\n" +
            "| 4 | 701     |  Name1 |" + "\r\n" +
            "| 4 | 701     |  Name2 |" + "\r\n" +
            "| 4 | 701     |  Name3 |" + "\r\n" +
            "| 4 | 701     |  Name4 |" + "\r\n" +
            "| 2 | 702     |  Name5 |" + "\r\n" +
            "| 2 | 702     |  Name6 |" + "\r\n" +
            "| 2 | 703     |  Name7 |" + "\r\n" +
            "| 2 | 703     |  Name8 |";

    String outputSrc =
            "|  user_id  |   names |" + "\r\n" +
            "|---------------------|" + "\r\n" +
            "|  701      |   Name1 |" + "\r\n" +
            "|  701      |   Name2 |" + "\r\n" +
            "|  701      |   Name3 |" + "\r\n" +
            "|  701      |   Name4 |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("3")),
            Arrays.asList(AggregationNode.AggrCount),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
