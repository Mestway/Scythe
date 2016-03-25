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
public class EnumTest30 {

    //I want to only get those Plan (with all their attributes) which have their IsActive attribute's value is set to True.

    String inputSrc =
            "|  Id  |  Plan  | Attributes  | Value  |" + "\r\n" +
            "|--------------------------------------|" + "\r\n" +
            "|  1   |   A    |   Name      |  AAA   |" + "\r\n" +
            "|  2   |   A    |   Class     |  P     |" + "\r\n" +
            "|  3   |   A    |   IsActive  |  True  |" + "\r\n" +
            "|  4   |   B    |   Name      |  BBB   |" + "\r\n" +
            "|  5   |   B    |   Class     |  Q     |" + "\r\n" +
            "|  6   |   B    |   IsActive  |  False |" + "\r\n" +
            "|  7   |   C    |   Name      |  CCC   |" + "\r\n" +
            "|  8   |   C    |   Class     |  R     |" + "\r\n" +
            "|  9   |   C    |   IsActive  |  True  |";


    String outputSrc =
            "|  Id |  Plan | Attributes  | Value  |" + "\r\n" +
            "|------------------------------------|" + "\r\n" +
            "|  1  |   A   |   Name      |  AAA   |" + "\r\n" +
            "|  2  |   A   |   Class     |  P     |" + "\r\n" +
            "|  3  |   A   |   IsActive  |  True  |" + "\r\n" +
            "|  7  |   C   |   Name      |  CCC   |" + "\r\n" +
            "|  8  |   C   |   Class     |  R     |" + "\r\n" +
            "|  9  |   C   |   IsActive  |  True  |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            2,
            Arrays.asList(Value.parse("True")),
            Arrays.asList(),
            2);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
