package forward_enumeration.hueristics;

import forward_enumeration.table_enumerator.hueristics.HeuristicNatJoin;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.table.NamedTable;
import util.TableInstanceParser;

/**
 * Created by clwang on 2/20/16.
 */
public class HeuristicTableJoinTest {


    String t1src =
            "| C_name    | F_key |" + "\r\n" +
            "|-------------------|" + "\r\n" +
            "| class1    |  f1   |" + "\r\n" +
            "| class2    |  f2   |" + "\r\n" +
            "| class3    |  f1   |" + "\r\n" +
            "| class4    |  f3   |" + "\r\n" +
            "| class5    |  f4   |";

    String t2src =
            "| S_key | C_name |" + "\r\n" +
            "|---------|" + "\r\n" +
            "|S1|class1|\r\n" +
            "|S2|class1|\r\n" +
            "|S3|class2|\r\n" +
            "|S3|class5|\r\n" +
            "|S4|class2|\r\n" +
            "|S4|class4|\r\n" +
            "|S5|class3|\r\n" +
            "|S6|class3|\r\n" +
            "|S6|class2|\r\n" +
            "|S7|class5|\r\n" +
            "|S8|class4|";

    @Test
    public void testEquiJoinTwo() throws Exception {

        Table t1 = TableInstanceParser.parseMarkDownTable("table1", t1src);
        Table t2 = TableInstanceParser.parseMarkDownTable("table2", t2src);

        System.out.print(HeuristicNatJoin.heuristicEquiJoinTwo(new NamedTable(t1), new NamedTable(t2)).snd.prettyPrint(0));
    }
}