package enumerator;

import org.junit.Test;
import sql.lang.DataType.DateVal;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/8/16.
 */
public class EnumTest07 {

    //The query I am having trouble with is getting a yrq where the start and end dates are completely within it.
    // Example: I want to find the yrq for the date range 2013-02-01 to 2013-02-15, which would be B233.

    String inputSrc =
            "| yrq  | start_date | end_date   |" + "\r\n" +
            "|--------------------------------|" + "\r\n" +
            "| B233 | 2013-01-07 | 2013-03-23 |" + "\r\n" +
            "| B232 | 2012-09-24 | 2012-12-20 |" + "\r\n" +
            "| B231 | 2012-06-25 | 2012-09-13 |" + "\r\n" +
            "| B124 | 2012-04-02 | 2012-06-21 |" + "\r\n" +
            "| B123 | 2012-01-09 | 2012-03-27 |";

    String outputSrc =
            "| col1 |" + "\r\n" +
            "|------|" + "\r\n" +
            "| B233 |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(Value.parse("2013-02-01"), Value.parse("2013-02-15")),
            Arrays.asList(),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
