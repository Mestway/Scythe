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
public class EnumTest22x {

    //I need the count of deviceid based on the number of timestamps it is seen in.
    // So the output would be something like: 0add is seen in 2 timestamps hence the count is 2 whereas 0bdd is seen in one time stamp hence 0bdd has count of 1.
    // The number of licenses corresponding to the device per time stamp is not considered for the count.


    String inputSrc =
            "|  id   |    date     | time_stamp | licenseid |  storeid | deviceid |value|" + "\r\n" +
            "|--------------------------------------------------------------------------|" + "\r\n" +
            "|  1    |  2015-06-12 |  17:36:15  | lic0001   |    1     |  0add    |  52 |" + "\r\n" +
            "|  2    |  2015-06-12 |  17:36:15  | lic0002   |    1     |  0add    |  54 |" + "\r\n" +
            "|  3    |  2015-06-12 |  17:36:15  | lic0003   |    1     |  0add    |  53 |" + "\r\n" +
            "|  4    |  2015-06-12 |  17:36:21  | lic0001   |    1     |  0add    |  54 |" + "\r\n" +
            "|  5    |  2015-06-12 |  17:36:21  | lic0002   |    1     |  0add    |  59 |" + "\r\n" +
            "|  6    |  2015-06-12 |  17:36:21  | lic0003   |    1     |  0add    |  62 |" + "\r\n" +
            "|  7    |  2015-06-12 |  17:36:21  | lic0004   |    1     |  0add    |  55 |" + "\r\n" +
            "|  8    |  2015-06-12 |  17:36:15  | lic0001   |    1     |  0bdd    |  53 |" + "\r\n" +
            "|  9    |  2015-06-12 |  17:36:15  | lic0002   |    1     |  0bdd    |  52 |" + "\r\n" +
            "|  10   |  2015-06-12 |  17:36:15  | lic0003   |    1     |  0bdd    |  52 |";



    String outputSrc =
            "| date        | deviceid | count |" + "\r\n" +
            "|--------------------------------|" + "\r\n" +
            "| 2015-06-12  | 0add     | 2     |" + "\r\n" +
            "| 2015-06-12  | 0bdd     | 1     |";

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
