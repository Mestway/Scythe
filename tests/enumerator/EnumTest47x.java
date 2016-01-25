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
public class EnumTest47x {

    //and I am looking for the id of the bill with the maximum amount per waiter.


    String inputSrc =
            "| id  | amount  | id_waiter |" + "\r\n" +
            "|---------------------------|" + "\r\n" +
            "| 1   | 20      | 1         |" + "\r\n" +
            "| 2   | 25      | 2         |" + "\r\n" +
            "| 3   | 50      | 2         |" + "\r\n" +
            "| 4   | 20      | 1         |" + "\r\n" +
            "| 5   | 60      | 1         |" + "\r\n" +
            "| 6   | 10      | 2         |";


    String outputSrc =
            "| waiter | maxamount | bill |" + "\r\n" +
            "|---------------------------|" + "\r\n" +
            "| 1      | 60        | 5    |" + "\r\n" +
            "| 2      | 50        | 3    |";

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
