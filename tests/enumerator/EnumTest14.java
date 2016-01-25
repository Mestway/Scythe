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
public class EnumTest14 {

    //I want is to get duplicates with the same email and name.


    String inputSrc =
            "|  ID |  NAME |  EMAIL       |" + "\r\n" +
            "|----------------------------|" + "\r\n" +
            "|  1  |  John |  asd@asd.com |" + "\r\n" +
            "|  2  |  Sam  |  asd@asd.com |" + "\r\n" +
            "|  3  |  Tom  |  asd@asd.com |" + "\r\n" +
            "|  4  |  Bob  |  bob@asd.com |" + "\r\n" +
            "|  5  |  Tom  |  asd@asd.com |";


    String outputSrc =
            "|  c  |" + "\r\n" +
            "|-----|" + "\r\n" +
            "| Tom |" + "\r\n" +
            "| Tom |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrCount),
            2);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
