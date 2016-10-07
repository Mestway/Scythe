package sql.lang.ast.table;

import org.testng.annotations.Test;
import sql.lang.Table;
import sql.lang.ast.Environment;
import util.Pair;
import util.TableInstanceParser;

import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * Created by clwang on 10/6/16.
 */
public class LeftJoinNodeTest {

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
    Table t1 = TableInstanceParser.parseMarkDownTable("t1", src);
    Table t2 = TableInstanceParser.parseMarkDownTable("t2", src2);

    @Test
    public void testEval() throws Exception {
        TableNode tn = new LeftJoinNode(new NamedTable(t1),
                new NamedTable(t2),
                Arrays.asList(new Pair<String, String>("t1.home", "t2.id")));

        System.out.println(tn.eval(new Environment()));
    }

}