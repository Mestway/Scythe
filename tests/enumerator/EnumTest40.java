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
public class EnumTest40 {

    //I need to fetch the data ignoring the least alt_bill_id if there are two rows for same acct_id.
    //In this case I need to ignore the row for acct_id 12345 and alt_bill_id 101. I need a result like the following:


    String inputSrc =
            "|  acct_id | Bill_Id |  Bill_dt   | alt_bill_id |" + "\r\n" +
            "|-----------------------------------------------|" + "\r\n" +
            "|  12345   |  123451 | 2014-01-02 |   101       |" + "\r\n" +
            "|  12345   |  123452 | 2014-01-02 |   102       |" + "\r\n" +
            "|  12346   |  123461 | 2014-01-02 |   103       |" + "\r\n" +
            "|  12347   |  123471 | 2014-01-02 |   104       |";


    String outputSrc =
            "|  acct_id | Bill_Id |   Bill_dt  | alt_bill_id |" + "\r\n" +
            "|-----------------------------------------------|" + "\r\n" +
            "|  12345   |  123452 | 2014-01-02 |  102        |" + "\r\n" +
            "|  12346   |  123461 | 2014-01-02 |  103        |" + "\r\n" +
            "|  12347   |  123471 | 2014-01-02 |  104        |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));

    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMin),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
