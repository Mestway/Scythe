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
public class EnumTest11 {

    // I need to select each distinct home holding the maximum value of datetime.

    String inputSrc =
            "| id | home|  datetime  | player  | resource |" + "\r\n" +
                    "|--------------------------------------------|" + "\r\n" +
                    "| 1  | 10  | 04/03/2009 | john    | 399      |" + "\r\n" +
                    "| 2  | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
                    "| 5  | 12  | 04/03/2009 | borat   | 555      |" + "\r\n" +
                    "| 3  | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
                    "| 4  | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n" +
                    "| 6  | 12  | 03/03/2009 | borat   | 500      |" + "\r\n" +
                    "| 7  | 13  | 24/12/2008 | borat   | 600      |" + "\r\n" +
                    "| 8  | 13  | 01/01/2009 | borat   | 700      |";


    String outputSrc = "| id | home|  datetime  | player | resource |" + "\r\n" +
            "|-------------------------------------------|" + "\r\n" +
            "| 1  | 10  | 04/03/2009 | john   | 399      |" + "\r\n" +
            "| 2  | 11  | 04/03/2009 | juliet | 244      |" + "\r\n" +
            "| 5  | 12  | 04/03/2009 | borat  | 555      |" + "\r\n" +
            "| 8  | 13  | 01/01/2009 | borat  | 700      |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMax),
            0);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
