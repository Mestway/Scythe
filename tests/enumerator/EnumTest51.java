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
public class EnumTest51 {

    //I want to select MAX value for every user, than it also lower than a tresshold 8.

    String inputSrc =
            "| User  | Phone | Value |" + "\r\n" +
            "|-----------------------|" + "\r\n" +
            "| Peter | 0     | 1     |" + "\r\n" +
            "| Peter | 456   | 2     |" + "\r\n" +
            "| Peter | 456   | 3     |" + "\r\n" +
            "| Paul  | 456   | 7     |" + "\r\n" +
            "| Paul  | 789   | 10    |";

    String outputSrc =
            "|  c1   | c2  | c3 |" + "\r\n" +
            "|------------------|" + "\r\n" +
            "| Peter | 456 | 3  |" + "\r\n" +
            "| Paul  | 456 | 7  |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("8")),
            Arrays.asList(AggregationNode.AggrMax),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
