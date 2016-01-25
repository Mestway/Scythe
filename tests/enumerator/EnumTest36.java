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
public class EnumTest36 {

    //I want to sum up the value field based on the ErrorName and return something like this:


    String inputSrc =
            "|  ErrorName | Value |" + "\r\n" +
            "|--------------------|" + "\r\n" +
            "|  Error1    | 3     |" + "\r\n" +
            "|  Error2    | 2     |" + "\r\n" +
            "|  Error3    | 2     |" + "\r\n" +
            "|  Error1    | 1     |" + "\r\n" +
            "|  Error2    | 1     |";

    String outputSrc =
            "| c1      | c2 |" + "\r\n" +
            "|--------------|" + "\r\n" +
            "| Error1  | 4  |" + "\r\n" +
            "| Error2  | 3  |" + "\r\n" +
            "| Error3  | 2  |";

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
