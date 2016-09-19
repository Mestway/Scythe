package forward_enumeration.hueristics;

import forward_enumeration.table_enumerator.hueristics.NaturalJoinInference;
import org.junit.Test;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

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
        List<TableNode> tryJoinResult = NaturalJoinInference.naturalJoinAll(
                Arrays.asList(TableInstanceParser.parseMarkDownTable("t1", t1),
                        TableInstanceParser.parseMarkDownTable("t2",t2)));

        DebugHelper.printTableNodes(tryJoinResult);
    }
}