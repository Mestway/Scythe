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
public class EnumTest19 {

    //How do I create a query that would give me the latest date for each user?
    //Update: I forgot that I needed to have a value that goes along with the latest date.


    String inputSrc =
            "| username |   date     | value|" + "\r\n" +
            "|------------------------------|" + "\r\n" +
            "| brad     | 2010-01-02 |  1.1 |" + "\r\n" +
            "| fred     | 2010-01-03 |  1.0 |" + "\r\n" +
            "| bob      | 2009-08-04 |  1.5 |" + "\r\n" +
            "| brad     | 2010-02-02 |  1.2 |" + "\r\n" +
            "| fred     | 2009-12-02 |  1.3 |";


    String outputSrc =
            "| username |   date      | value|" + "\r\n" +
            "|-------------------------------|" + "\r\n" +
            "| fred     | 2010-01-03  |  1.0 |" + "\r\n" +
            "| bob      | 2009-08-04  |  1.5 |" + "\r\n" +
            "| brad     | 2010-02-02  |  1.2 |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin),
            1);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
