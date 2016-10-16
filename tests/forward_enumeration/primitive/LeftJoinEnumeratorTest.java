package forward_enumeration.primitive;

import forward_enumeration.canonical_enum.components.EnumFilterNamed;
import forward_enumeration.context.EnumConfig;
import forward_enumeration.context.EnumContext;
import org.testng.annotations.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.LeftJoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.Pair;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * Created by clwang on 10/7/16.
 */
public class LeftJoinEnumeratorTest {

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
    public void testEnumLeftJoin() throws Exception {
        EnumConfig c = new EnumConfig(
                2,
                new ArrayList<>(),
                new ArrayList<>(),
                2);
        EnumContext ec = new EnumContext(Arrays.asList(t1, t2), c);
        DebugHelper.printList(LeftJoinEnumerator.enumLeftJoinFromEC(ec));
    }


    String t3Src =
            "| id | name  | url  | data_date  |" +"\r\n" +
            "|--------------------------------|" +"\r\n" +
            "| 1a | name1 | url1 | 2016-08-08 |" +"\r\n" +
            "| 1b | name2 | url2 | 2016-08-08 |" +"\r\n" +
            "| 1c | name3 | url3 | 2016-08-08 |" +"\r\n" +
            "| 1a | name1 | url1 | 2016-08-09 |" +"\r\n" +
            "| 1b | name2 | url2 | 2016-08-09 |" +"\r\n" +
            "| 1c | name3 | url3 | 2016-08-09 |";

    String t4Src =
            "| id | views | data_date |" +"\r\n" +
            "|------------------------|" +"\r\n" +
            "| 1a | 10   | 2016-08-08 |" +"\r\n" +
            "| 1b | 15   | 2016-08-08 |" +"\r\n" +
            "| 1a | 12   | 2016-08-09 |" +"\r\n" +
            "| 1b | 17   | 2016-08-09 |";

    String t5Src =
            "| url | views | data_date |" +"\r\n" +
            "|------------------------|" +"\r\n" +
            "| url3 | 22 | 2016-08-08 |" +"\r\n" +
            "| url3 | 12 | 2016-08-09 |";

    Table t3 = TableInstanceParser.parseMarkDownTable("t3", t3Src);
    Table t4 = TableInstanceParser.parseMarkDownTable("t4", t4Src);
    Table t5 = TableInstanceParser.parseMarkDownTable("t5", t5Src);

    @Test
    public void testEnumLeftJoin2() throws Exception {
        EnumConfig c = new EnumConfig(
                2,
                new ArrayList<>(),
                new ArrayList<>(),
                2);
        EnumContext ec = new EnumContext(Arrays.asList(t3, t4, t5), c);
        //DebugHelper.printTableNodes(LeftJoinEnumerator.enumLeftJoinFromEC(ec).stream().map(t -> ((TableNode) t)).collect(Collectors.toList()));

        TableNode lj1 = new LeftJoinNode(new NamedTable(t3), new NamedTable(t4), Arrays.asList(new Pair<String, String>("t3.id", "t4.id")));
        TableNode lj2 = new LeftJoinNode(new NamedTable(t3), new NamedTable(t4), Arrays.asList(new Pair<String, String>("t3.id", "t4.id"), new Pair<String, String>("t3.data_date", "t4.data_date")));

        DebugHelper.printTableNodes(Arrays.asList(lj1, lj2));

        System.out.println();


    }
}