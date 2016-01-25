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
public class EnumTest31x {

    // idea, try to do aggregation after aggregation

    //I've tried to find a pure sql solution so that i can see: in each city, which colour of car has the largest quantity.


    String inputSrc0 =
            "|   city      |  car_colour |  car_type |  qty     | cnt |" + "\r\n" +
            "|--------------------------------------------------------|" + "\r\n" +
            "| manchester  |  Red        |  Sports   |  7       |  2 |" + "\r\n" +
            "| manchester  |  Red        |  4x4      |  9       |  2 |" + "\r\n" +
            "| manchester  |  Blue       |  4x4      |  8       |  1 |" + "\r\n" +
            "| london      |  Red        |  Sports   |  2       |  1 |" + "\r\n" +
            "| london      |  Blue       |  4x4      |  3       |  1 |" + "\r\n" +
            "| leeds       |  Red        |  Sports   |  5       |  1 |" + "\r\n" +
            "| leeds       |  Blue       |  Sports   |  6       |  2 |" + "\r\n" +
            "| leeds       |  Blue       |  4X4      |  1       |  2 |";

    String inputSrc =
            "|   city      |  car_colour |  car_type |  qty      |" + "\r\n" +
            "|--------------------------------------------------------|" + "\r\n" +
            "| manchester  |  Red        |  Sports   |  7        |" + "\r\n" +
            "| manchester  |  Red        |  4x4      |  9        |" + "\r\n" +
            "| manchester  |  Blue       |  4x4      |  8        |" + "\r\n" +
            "| london      |  Red        |  Sports   |  2        |" + "\r\n" +
            "| london      |  Blue       |  4x4      |  3        |" + "\r\n" +
            "| leeds       |  Red        |  Sports   |  5        |" + "\r\n" +
            "| leeds       |  Blue       |  Sports   |  6        |" + "\r\n" +
            "| leeds       |  Blue       |  4X4      |  1        |";

    String outputSrc =
            "| c1         | c2   |" + "\r\n" +
            "|-------------------|" + "\r\n" +
            "| manchester | Red  |" + "\r\n" +
            "| london     | Red  |" + "\r\n" +
            "| london     | Blue |" + "\r\n" +
            "| leeds      | Blue |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrCount),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
