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
public class EnumTest33 {

    //What i would like to select is the ARIDNR that occurs more than on times. With the different LIEFNR.


    String inputSrc =
            "|ARIDNR|LIEFNR|" + "\r\n" +
            "|-------------|" + "\r\n" +
            "|1     |A     |" + "\r\n" +
            "|2     |A     |" + "\r\n" +
            "|3     |A     |" + "\r\n" +
            "|1     |B     |" + "\r\n" +
            "|2     |B     |";


    String outputSrc =
            "|ARIDNR|LIEFNR|" + "\r\n" +
            "|-------------|" + "\r\n" +
            "|1     |A     |" + "\r\n" +
            "|1     |B     |" + "\r\n" +
            "|2     |A     |" + "\r\n" +
            "|2     |B     |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("2")),
            Arrays.asList(AggregationNode.AggrCount),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
