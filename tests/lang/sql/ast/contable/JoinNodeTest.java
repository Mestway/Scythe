package lang.sql.ast.contable;

import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.exception.SQLEvalException;
import org.junit.Test;
import util.TableExampleParser;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by clwang on 12/19/15.
 */
public class JoinNodeTest {
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

    String src2 =
            "| id | tid   |" + "\r\n" +
            "|------------|" + "\r\n" +
            "| 1  | a     |" + "\r\n" +
            "| 5  | c     |";
    Table t1 = TableExampleParser.parseMarkDownTable("table1", src);
    Table t2 = TableExampleParser.parseMarkDownTable("table2", src2);

    String resultSrc =
            "|table1.id | table1.broId | table1.home | table1.datetime | table1.player | table1.resource | table2.id | table2.tid|" + "\r\n" +
            "|-------------------------------------------------------------------------|" + "\r\n" +
            "|1.0 | 1.0 | 10.0 | 04/03/2009 | john | 399.0 | 1.0 | a|" + "\r\n" +
            "|1.0 | 1.0 | 10.0 | 04/03/2009 | john | 399.0 | 5.0 | c|" + "\r\n" +
            "|2.0 | 3.0 | 11.0 | 04/03/2009 | juliet | 244.0 | 1.0 | a|" + "\r\n" +
            "|2.0 | 3.0 | 11.0 | 04/03/2009| juliet | 244.0 | 5.0 | c|" + "\r\n" +
            "|5.0 | 4.0 | 12.0 | 04/03/2009 | borat | 555.0 | 1.0 | a|" + "\r\n" +
            "|5.0 | 4.0 | 12.0 | 04/03/2009 | borat | 555.0 | 5.0 | c|" + "\r\n" +
            "|3.0 | 5.0 | 10.0 | 03/03/2009 | john | 300.0 | 1.0 | a|" + "\r\n" +
            "|3.0 | 5.0 | 10.0 | 03/03/2009 | john | 300.0 | 5.0 | c|" + "\r\n" +
            "|4.0 | 7.0 | 11.0 | 03/03/2009 | juliet | 200.0 | 1.0 | a|" + "\r\n" +
            "|4.0 | 7.0 | 11.0 | 03/03/2009 | juliet | 200.0 | 5.0 | c|" + "\r\n" +
            "|6.0 | 6.0 | 12.0 | 03/03/2009 | borat | 500.0 | 1.0 | a|" + "\r\n" +
            "|6.0 | 6.0 | 12.0 | 03/03/2009 | borat | 500.0 | 5.0 | c|" + "\r\n" +
            "|7.0 | 5.0 | 13.0 | 24/12/2008 | borat | 600.0 | 1.0 | a|" + "\r\n" +
            "|7.0 | 5.0 | 13.0 | 24/12/2008 | borat | 600.0 | 5.0 | c|" + "\r\n" +
            "|8.0 | 8.0 | 13.0 | 01/01/2009 | borat | 700.0 | 1.0 | a|" + "\r\n" +
            "|8.0 | 8.0 | 13.0 | 01/01/2009 | borat | 700.0 | 5.0 | c |";

    Table resultTable = TableExampleParser.parseMarkDownTable("anonymous", resultSrc);

    @Test
    public void joinTest1() {
        TableNode tn = new JoinNode(
                Arrays.asList
                        (new NamedTableNode(t1),
                                new NamedTableNode(t2))
        );
        try {
            assertTrue(resultTable.contentEquals(tn.eval(new Environment())));
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }
}