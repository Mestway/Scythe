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
public class EnumTest50x {

    /*The validity of a time zone is depends on time_start field in the database. This is important to get the correct time_start.
    The example below shows how to query the time zone information using zone name America/Los_Angeles.

            SELECT * FROM timezone
    WHERE time_start < strftime('%s', 'now')
    AND zone_name='America/Los_Angeles'
    ORDER BY time_start DESC LIMIT 1;
    This query returns

    391|America/Los_Angeles|1425808800
    I'd like to to do the same thing but for all zone_id with one SQL Query.
    */

    String inputSrc =
            "| zone_id | zone_name           | time_start |" + "\r\n" +
            "|--------------------------------------------|" + "\r\n" +
            "| 391     | America/Los_Angeles | 2147397247 |" + "\r\n" +
            "| 391     | America/Los_Angeles | 1425808800 |" + "\r\n" +
            "| 391     | America/Los_Angeles | 2140678800 |" + "\r\n" +
            "| 391     | America/Los_Angeles | 9972000    |" + "\r\n" +
            "| 392     | America/Metlakatla  | 2147397247 |" + "\r\n" +
            "| 392     | America/Metlakatla  | 436352400  |" + "\r\n" +
            "| 392     | America/Metlakatla  | 9972000    |" + "\r\n" +
            "| 393     | America/Anchorage   | 2147397247 |" + "\r\n" +
            "| 393     | America/Anchorage   | 2140682400 |" + "\r\n" +
            "| 393     | America/Anchorage   | 2120122800 |" + "\r\n" +
            "| 393     | America/Anchorage   | 1425812400 |" + "\r\n" +
            "| 393     | America/Anchorage   | 9979200    |";


    String outputSrc =
            "| c1  | c2                  |   c3         |" + "\r\n" +
            "|------------------------------------------|" + "\r\n" +
            "| 391 | America/Los_Angeles | 1425808800   |" + "\r\n" +
            "| 392 | America/Metlakatla  | 436352400    |" + "\r\n" +
            "| 393 | America/Anchorage   | 1425812400   |";

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
