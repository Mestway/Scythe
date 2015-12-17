package sql.lang.query;

import org.junit.Test;
import sql.lang.DataType.IntVal;
import sql.lang.DataType.Value;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.val.NamedVal;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by clwang on 12/14/15.
 */
public class SQLQueryTest {

    @Test
    public void testSelect() throws Exception {
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

        Table testTable = TableInstanceParser.parseMarkDownTable("input", src);

        String[] proj = {"id", "broId", "datetime", "player"};
        List<String> projectionGoal = Arrays.asList(proj);

        /**
         * SELECT input.id, input.broId, input.datetime, input.player
         * FROM input
         * WHERE input.id < input.broId
         */
        SQLQuery query = new SQLQuery(
                new SelectNode(
                        new NamedTable(testTable),
                        new VVComparator(Arrays.asList(new NamedVal("input.id"), new NamedVal("input.broId")),VVComparator.lt)
                )
        );

        String result1 =
                "| id | broId | home|  datetime  | player  | resource |" + "\r\n" +
                "|----------------------------------------------------|" + "\r\n" +
                "| 2  | 3     | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
                "| 3  | 5     | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
                "| 4  | 7     | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n";

        Table resultTable1 = TableInstanceParser.parseMarkDownTable("result1", result1);

        assertTrue(query.execute().tableEquals(resultTable1));

        /**
         * SELECT input.id, input.broId, input.datetime, input.player
         * FROM input
         * WHERE resource
         */
        SQLQuery query2 = new SQLQuery(
                new SelectNode(
                        new NamedTable(testTable),
                        new VVComparator(Arrays.asList(new NamedVal("id"), new NamedVal("broId")),VVComparator.lt)
                )
        );

        String result2 =
                "| id | broId |  datetime  | player  |" + "\r\n" +
                "|-----------------------------------|" + "\r\n" +
                "| 2  | 3     | 04/03/2009 | juliet  |" + "\r\n" +
                "| 3  | 5     | 03/03/2009 | john    |" + "\r\n" +
                "| 4  | 7     | 03/03/2009 | juliet  |";
        Table resultTable = TableInstanceParser.parseMarkDownTable("resultTable", result2);


    }
}