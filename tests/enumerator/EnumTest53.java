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
public class EnumTest53 {

    //What I need is the results to only contain the row with the greatest value
    //   in the quantity column and also the row with the greatest retail_price.
    // So my result set I need would look like this

    String inputSrc =
            "| number|quantity|retail_price|" + "\r\n" +
            "|-----------------------------|" + "\r\n" +
            "| 1007  | 288    | 5.750      |" + "\r\n" +
            "| 1007  | 48     | 5.510      |" + "\r\n" +
            "| 1007  | 576    | 5.460      |" + "\r\n" +
            "| 1007  | 96     | 5.240      |" + "\r\n" +
            "| 1007  | 576    | 5.230      |" + "\r\n" +
            "| 1007  | 144    | 5.120      |" + "\r\n" +
            "| 1006  | 200    | 5.760      |" + "\r\n" +
            "| 1006  | 100    | 5.550      |" + "\r\n" +
            "| 1006  | 200    | 5.040      |" + "\r\n" +
            "| 1006  | 500    | 5.010      |";

    String outputSrc =
            "| number|quantity|retail_price|" + "\r\n" +
            "|-----------------------------|" + "\r\n" +
            "| 1006  | 500    | 5.010      |" + "\r\n" +
            "| 1007  | 576    | 5.460      |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMax),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
