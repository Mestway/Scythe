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
public class EnumTest44 {

    //And I would like to make a request that would return for each deal_id the row with the highest timestamp,
    // and the corresponding status_id.


    String inputSrc =
            "| deal_id  | status_id|  timestamp           |" + "\r\n" +
            "|--------------------------------------------|" + "\r\n" +
            "| 1226     |    1     |  2009-08-17 09:29:00 |" + "\r\n" +
            "| 1226     |    2     |  2009-08-17 17:01:56 |" + "\r\n" +
            "| 1226     |    2     |  2009-08-18 12:10:07 |" + "\r\n" +
            "| 1226     |    3     |  2009-08-18 12:10:25 |" + "\r\n" +
            "| 1226     |    4     |  2009-08-17 15:52:19 |" + "\r\n" +
            "| 1227     |    1     |  2009-08-17 09:56:31 |" + "\r\n" +
            "| 1227     |    2     |  2009-08-17 14:31:25 |";

    String outputSrc =
            "| c1    | c2 | c3                  |" + "\r\n" +
            "|----------------------------------|" + "\r\n" +
            "| 1226  | 3  | 2009-08-18 12:10:25 |" + "\r\n" +
            "| 1227  | 2  | 2009-08-17 14:31:25 |";

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
