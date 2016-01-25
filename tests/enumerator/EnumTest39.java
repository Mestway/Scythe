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
public class EnumTest39 {

    //The idea is to sum the observation by Object and Date.
    // Ideally what I would like to do though is to make it so that rather
    // than getting a numeric month, mm, I get a date mm/dd/yyyy.


    String inputSrc =
            "| Object | Observation | Date       |" + "\r\n" +
            "|-----------------------------------|" + "\r\n" +
            "| 1      | 215         | 10/01/2015 |" + "\r\n" +
            "| 2      | 125         | 10/01/2015 |" + "\r\n" +
            "| 1      | 225         | 10/04/2015 |" + "\r\n" +
            "| 2      | 150         | 10/04/2015 |" + "\r\n" +
            "| 1      | 250         | 10/08/2015 |";


    String outputSrc =
            "| Object | Total  |   Date    |" + "\r\n" +
            "|-----------------------------|" + "\r\n" +
            "|  1     | 690    | 10/01/2015 |" + "\r\n" +
            "|  2     | 275    | 10/01/2015 |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMin, AggregationNode.AggrSum),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
