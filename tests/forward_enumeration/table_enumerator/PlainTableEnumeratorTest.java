package forward_enumeration.table_enumerator;

import forward_enumeration.context.EnumConfig;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 3/21/16.
 */
public class PlainTableEnumeratorTest {
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

    EnumConfig c = new EnumConfig(
            1,
            new ArrayList<>(),
            Arrays.asList(AggregationNode.AggrMin),
            0,
            Arrays.asList(input));

    @Test
    public void test1() {
        List<TableNode> tn = new PlainTableEnumerator().enumProgramWithIO(Arrays.asList(input), output, c);
        //DebugHelper.printTableNodes(tn);
    }
    @Test
    public void test2() {
        List<TableNode> tn = new CanonicalTableEnumerator().enumProgramWithIO(Arrays.asList(input), output, c);
        //DebugHelper.printTableNodes(tn);
    }

}