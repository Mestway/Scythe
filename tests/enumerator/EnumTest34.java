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
public class EnumTest34 {

    //I try to figure out the people who went to several locations.


    String inputSrc =
            "| Customer    | email          |   ZIP | shop |" + "\r\n" +
            "|---------------------------------------------|" + "\r\n" +
            "| John Smith  | js@mail.com    | 75016 |    1 |" + "\r\n" +
            "| Mary King   | mary@ymail.com | 97430 |    2 |" + "\r\n" +
            "| John Smith  | js@mail.com    | 75016 |    3 |" + "\r\n" +
            "| Ivan Turtle | ivan@mail.com  | 56266 |    5 |" + "\r\n" +
            "| Mary King   | mary@ymail.com | 97430 |    5 |" + "\r\n" +
            "| John Smith  | js@mail.com    | 75016 |    5 |";


    String outputSrc =
            "| c1         |" + "\r\n" +
            "|------------|" + "\r\n" +
            "| John Smith |" + "\r\n" +
            "| Mary King  |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("1")),
            Arrays.asList(AggregationNode.AggrCount),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
