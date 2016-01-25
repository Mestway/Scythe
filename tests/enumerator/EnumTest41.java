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
public class EnumTest41 {

    //I'm trying to list the latest destination (MAX departure time) for each train in a table


    String inputSrc =
            "| Train | Dest | Time  |" + "\r\n" +
            "|----------------------|" + "\r\n" +
            "| 1     | HK   | 10:00 |" + "\r\n" +
            "| 1     | SH   | 12:00 |" + "\r\n" +
            "| 1     | SZ   | 14:00 |" + "\r\n" +
            "| 2     | HK   | 13:00 |" + "\r\n" +
            "| 2     | SH   | 09:00 |" + "\r\n" +
            "| 2     | SZ   | 07:00 |";

    String outputSrc =
            "| Train | Dest | Time  |" + "\r\n" +
            "|----------------------|" + "\r\n" +
            "| 1     | SZ   | 14:00 |" + "\r\n" +
            "| 2     | HK   | 13:00 |";

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
