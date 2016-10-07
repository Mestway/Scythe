package forward_enumeration.primitive;

import forward_enumeration.canonical_enum.components.EnumFilterNamed;
import forward_enumeration.context.EnumConfig;
import forward_enumeration.context.EnumContext;
import org.testng.annotations.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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


    EnumConfig c = new EnumConfig(
            2,
            new ArrayList<>(),
            new ArrayList<>(),
            2);
    EnumContext ec = new EnumContext(Arrays.asList(t1, t2), c);

    @Test
    public void testEnumLeftJoin() throws Exception {
        DebugHelper.printList(LeftJoinEnumerator.enumLeftJoinFromEC(ec));
    }

}