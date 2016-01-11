package enumerator;

import org.junit.Test;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/7/16.
 */
public class EnumTest02 {
    String inputSrc =
            "| locId  |    dtg                |  temp |" + "\r\n" +
            "|----------------------------------------|" + "\r\n" +
            "| 100    |  2009-02-25 10:00:00  |  15   |" + "\r\n" +
            "| 200    |  2009-02-25 10:00:00  |  20   |" + "\r\n" +
            "| 300    |  2009-02-25 10:00:00  |  24   |" + "\r\n" +
            "| 100    |  2009-02-25 09:45:00  |  13   |" + "\r\n" +
            "| 300    |  2009-02-25 09:45:00  |  16   |" + "\r\n" +
            "| 200    |  2009-02-25 09:45:00  |  18   |" + "\r\n" +
            "| 400    |  2009-02-25 09:45:00  |  12   |" + "\r\n" +
            "| 100    |  2009-02-25 09:30:00  |  11   |" + "\r\n" +
            "| 300    |  2009-02-25 09:30:00  |  14   |" + "\r\n" +
            "| 200    |  2009-02-25 09:30:00  |  15   |" + "\r\n" +
            "| 400    |  2009-02-25 09:30:00  |  10   |";

    String outputSrc =
            "| locID |    dtg                  | tmp |" + "\r\n" +
            "|---------------------------------------|" + "\r\n" +
            "| 100   |   2009-02-25 10:00:00   | 15  |" + "\r\n" +
            "| 200   |   2009-02-25 10:00:00   | 20  |" + "\r\n" +
            "| 300   |   2009-02-25 10:00:00   | 24  |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(1,
            Arrays.asList(Value.parse("2009-02-25 09:50:00")),
            Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin));

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
