package enumerator;

import org.junit.Test;
import sql.lang.DataType.Value;
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
public class EnumTest03 {
    String inputSrc =
            "| ID  | Name            | City           | Birthyear  |\r\n" +
            "|-----------------------------------------------------|\r\n" +
            "| 1   | Egon Spengler   | New York       | 1957       |\r\n" +
            "| 2   | Mac Taylor      | New York       | 1955       |\r\n" +
            "| 3   | Sarah Connor    | Los Angeles    | 1959       |\r\n" +
            "| 4   | Jean-Luc Picard | La Barre       | 2305       |\r\n" +
            "| 5   | Ellen Ripley    | Nostromo       | 2092       |\r\n" +
            "| 6   | James T. Kirk   | Riverside      | 2233       |\r\n" +
            "| 7   | Henry Jones     | Chicago        | 1899       |";

    String outputSrc =
            "| col1          |  col2       |\r\n" +
            "|-----------------------------|\r\n" +
            "| Henry Jones   | Chicago     |\r\n" +
            "| Mac Taylor    | New York    |\r\n" +
            "| Sarah Connor  | Los Angeles |\r\n" +
            "|Jean-Luc Picard | La Barre   |\r\n" +
            "| Ellen Ripley    | Nostromo  |\r\n" +
            "| James T. Kirk   | Riverside |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin),
            1);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }

}
