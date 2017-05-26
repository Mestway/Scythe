package forward_enumeration.hueristics;

import org.junit.Test;
import lang.sql.ast.contable.TableNode;
import util.DebugHelper;
import util.TableExampleParser;
import util.hueristics.HeuristicNatJoin;

import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/24/16.
 */
public class TableJoinInferenceTest {

    String t1 =
            "| VehicleID | Name |" + "\r\n" +
            "|------------------|" + "\r\n" +
            "| 1         | Chuck|" + "\r\n" +
            "| 2         | Larry|";

    String t2 =
            "| LocationID | VehicleID |  City        |" + "\r\n" +
            "|---------------------------------------|" + "\r\n" +
            "| 1          | 1         |  New York    |" + "\r\n" +
            "| 2          | 1         |  Seattle     |" + "\r\n" +
            "| 3          | 1         |  Vancouver   |" + "\r\n" +
            "| 4          | 2         |  Los Angeles |" + "\r\n" +
            "| 5          | 2         |  Houston     |";

    @Test
    public void testNaturalJoinAll() throws Exception {
        List<TableNode> tryJoinResult = HeuristicNatJoin.naturalJoinAll(
                Arrays.asList(TableExampleParser.parseMarkDownTable("t1", t1),
                        TableExampleParser.parseMarkDownTable("t2",t2)));

        DebugHelper.printTableNodes(tryJoinResult);
    }
}