package enumerator;

import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/8/16.
 */
public class EnumTest04 {
    String inputSrc =
            "| id  | customer | total |\r\n" +
            "|------------------------|\r\n" +
            "| 1   | Joe      | 5     |\r\n " +
            "| 2   | Sally    | 3     |\r\n" +
            "| 3   | Joe      | 2     |\r\n" +
            "| 4   | Sally    | 1     |";

    String outputSrc =
            "| FIRST(id) | customer | FIRST(total) |\r\n" +
            "|-------------------------------------|\r\n " +
            "|         1 | Joe      | 5            |\r\n " +
            "|         2 | Sally    | 3            |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(1, new ArrayList<>(), Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin));

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
