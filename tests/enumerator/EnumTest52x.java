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
public class EnumTest52x {

    /*The current query would be:

    SELECT Person, SUM(Time) AS Duration
    FROM Table
    GROUP BY Person

    What I need to add to this result set is the Uniq and value of the largest value per person
    */

    String inputSrc =
        "| Uniq |  Value | Time | Person |" + "\r\n" +
        "|-------------------------------|" + "\r\n" +
        "|   1  |  6     | 180  | Bob    |" + "\r\n" +
        "|   2  |  8     | 170  | Bob    |" + "\r\n" +
        "|   3  |  4     | 45   | Claire |" + "\r\n" +
        "|   4  |  4     | 90   | Claire |";

    String outputSrc =
        "| Person | Duration | Value | Uniq |" + "\r\n" +
        "|----------------------------------|" + "\r\n" +
        "| Bob    | 350      | 8     | 2    |" + "\r\n" +
        "| Claire | 135      | 4     | 3    |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("8")),
            Arrays.asList(AggregationNode.AggrSum, AggregationNode.AggrMax, AggregationNode.AggrMin),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
