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
public class EnumTest05 {
    String inputSrc =
            "| Id |  Name |  Other_Columns | \r\n" +
                    "|-----------------------------| \r\n" +
                    "| 1  |  A    |   A_data_1     | \r\n" +
                    "| 2  |  A    |   A_data_2     | \r\n" +
                    "| 3  |  A    |   A_data_3     | \r\n" +
                    "| 4  |  B    |   B_data_1     | \r\n" +
                    "| 5  |  B    |   B_data_2     | \r\n" +
                    "| 6  |  C    |   C_data_1     |";

    String outputSrc =
            "| col1 | col2 | col3     | \r\n" +
            "|------------------------| \r\n" +
            "| 3    | A    | A_data_3 | \r\n" +
            "| 5    | B    | B_data_2 | \r\n" +
            "| 6    | C    | C_data_1 |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            new ArrayList<>(),
            Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin),
            1);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
