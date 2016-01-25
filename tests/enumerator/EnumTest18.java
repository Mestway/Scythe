package enumerator;

import org.junit.Test;
import sql.lang.DataType.StringVal;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.ValNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/11/16.
 */
public class EnumTest18 {
    String inputSrc =
            "| id | country| status       |  \r\n" +
            "|----------------------------|  \r\n" +
            "| 1  | SE     | TREATED      |  \r\n" +
            "| 2  | DK     | UNTREATED    |  \r\n" +
            "| 3  | SE     | UNTREATED    |";

    String outputSrc =
            "| id | country| status | + \r\n" +
            "|----------------------------|+ \r\n" +
            "| 1  | SE     | TREATED      |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);
    Constraint c = new Constraint(
            1,
            Arrays.asList(new StringVal("UNTREATED"), new StringVal("TREATED")),
            new ArrayList<>(),
            2);//Arrays.asList(AggregationNode.AggrCount));

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
