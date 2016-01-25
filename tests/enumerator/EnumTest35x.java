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
public class EnumTest35x {

    //I want to know which stores sell the same fruit and what that fruit is.


    String inputSrc =
            "|  ID |  Fruit    |" + "\r\n" +
            "|-----------------|" + "\r\n" +
            "|  1  |  apples   |" + "\r\n" +
            "|  1  |  bananas  |" + "\r\n" +
            "|  2  |  apples   |" + "\r\n" +
            "|  2  |  oranges  |" + "\r\n" +
            "|  2  |  cherries |" + "\r\n" +
            "|  2  |  lychees  |" + "\r\n" +
            "|  3  |  bananas  |" + "\r\n" +
            "|  3  |  cherries |" + "\r\n" +
            "|  3  |  lychees  |";


    String outputSrc =
            "|  ID1 | ID2 | Fruit    |" + "\r\n" +
            "|-----------------------|" + "\r\n" +
            "|  1   | 2   | apples   |" + "\r\n" +
            "|  1   | 3   | bananas  |" + "\r\n" +
            "|  2   | 3   | cherries |" + "\r\n" +
            "|  2   | 3   | lychees  |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrCount),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
