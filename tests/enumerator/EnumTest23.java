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
public class EnumTest23 {

    //I want to count how many of each type that has 'Error' as status

    String inputSrc =
            "|   Id    |    Name    | Status | Content_type |" + "\r\n" +
            "|----------------------------------------------|" + "\r\n" +
            "| 2960671 | PostJob    | Error  | general_url  |" + "\r\n" +
            "| 2960670 | auto_index | Done   | general_url  |" + "\r\n" +
            "| 2960669 | auto_index | Done   | document     |" + "\r\n" +
            "| 2960668 | auto_index | Error  | document     |" + "\r\n" +
            "| 2960667 | auto_index | Error  | document     |";


    String outputSrc =
            "| c1  | c2           |" + "\r\n" +
            "|--------------------|" + "\r\n" +
            "| 1   |  general_url |" + "\r\n" +
            "| 2   |   document   |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("Error")),
            Arrays.asList(AggregationNode.AggrCount),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
