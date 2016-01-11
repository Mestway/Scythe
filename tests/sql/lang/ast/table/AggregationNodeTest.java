package sql.lang.ast.table;

import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.exception.SQLEvalException;
import util.TableInstanceParser;

import java.util.Arrays;

/**
 * Created by clwang on 12/22/15.
 */
public class AggregationNodeTest {
    String src =
                    "| id | broId | home|  datetime  | player  | resource |" + "\r\n" +
                    "|----------------------------------------------------|" + "\r\n" +
                    "| 1  | 1     | 10  | 04/03/2009 | john    | 399      |" + "\r\n" +
                    "| 2  | 3     | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
                    "| 5  | 4     | 12  | 04/03/2009 | borat   | 555      |" + "\r\n" +
                    "| 3  | 5     | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
                    "| 4  | 7     | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n" +
                    "| 6  | 6     | 12  | 03/03/2009 | borat   | 500      |" + "\r\n" +
                    "| 7  | 5     | 13  | 24/12/2008 | borat   | 600      |" + "\r\n" +
                    "| 8  | 8     | 13  | 01/01/2009 | borat   | 700      |";

    Table t1 = TableInstanceParser.parseMarkDownTable("table1", src);

    @Test
    public void Test1() {
        AggregationNode agrNode = new AggregationNode(
                AggregationNode.AggrMax,
                new NamedTable(t1),
                Arrays.asList("table1.home"),
                "table1.resource");

        try {
            System.out.println(agrNode.eval(new Environment()));
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void Test2() {
        AggregationNode agrNode = new AggregationNode(
                AggregationNode.AggrMax,
                new NamedTable(t1),
                Arrays.asList("table1.home", "table1.player"),
                "table1.resource");

        try {
            System.out.println(agrNode.eval(new Environment()));
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }
}