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
public class EnumTest49 {

    //I want to select the 3 max payments grouped by the couple firstname, lastname

    String inputSrc =
            "| firstname | lastname | nb_payments |" + "\r\n" +
            "|------------------------------------|" + "\r\n" +
            "| a         | b        | 10          |" + "\r\n" +
            "| a         | b        | 20          |" + "\r\n" +
            "| b         | a        | 30          |" + "\r\n" +
            "| b         | a        | 40          |" + "\r\n" +
            "| b         | b        | 50          |";

    String outputSrc =
            "| firstname | lastname | top3 |" + "\r\n" +
            "|-----------------------------|" + "\r\n" +
            "| b         | a        | 70   |" + "\r\n" +
            "| b         | b        | 50   |" + "\r\n" +
            "| a         | b        | 30   |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrSum),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
