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
public class EnumTest20 {

    //How do I create a query that would give me the latest date for each user?
    //Update: I forgot that I needed to have a value that goes along with the latest date.


    String inputSrc1 =
            "| VehicleID | Name |" + "\r\n" +
            "|------------------|" + "\r\n" +
            "| 1         | Chuck|" + "\r\n" +
            "| 2         | Larry|";

    String inputSrc2 =
            "| LocationID | VehicleID |  City        |" + "\r\n" +
            "|---------------------------------------|" + "\r\n" +
            "| 1          | 1         |  New York    |" + "\r\n" +
            "| 2          | 1         |  Seattle     |" + "\r\n" +
            "| 3          | 1         |  Vancouver   |" + "\r\n" +
            "| 4          | 2         |  Los Angeles |" + "\r\n" +
            "| 5          | 2         |  Houston     |";


    String outputSrc =
            "| VehicleID  | Name   | Locations                     |" + "\r\n" +
            "|-----------------------------------------------------|" + "\r\n" +
            "| 1          | Chuck  | New York, Seattle, Vancouver  |" + "\r\n" +
            "| 2          | Larry  | Los Angeles, Houston          |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc1),
            TableInstanceParser.parseMarkDownTable("table2", inputSrc2));
    Table output = TableInstanceParser.parseMarkDownTable("table3", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrConcat),
            2);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
