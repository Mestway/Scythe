package enumerator;

import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by clwang on 2/8/16.
 */
public class TableEnumeratorTest {

    String inputSrc =
            "| id   |  rev   |  content  |" + "\r\n" +
                    "|---------------------------|" + "\r\n" +
                    "| 1    |  1     |  A        |" + "\r\n" +
                    "| 2    |  1     |  B        |" + "\r\n" +
                    "| 1    |  2     |  C        |" + "\r\n" +
                    "| 1    |  3     |  D        |";

    String outputSrc =
            "| col1 | col2 | col3 |" + "\r\n" +
                    "|--------------------|" + "\r\n" +
                    "|  1   |  3   |  D   |" + "\r\n" +
                    "|  2   |  1   |  B   |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);
    Constraint c = new Constraint(
            2,
            new ArrayList<>(),
            Arrays.asList(
                    AggregationNode.AggrMax),
            2);

    @Test
    public void testEnumTable() throws Exception {
        EnumContext ec = new EnumContext(Arrays.asList(input), c);
        List<TableNode> tns = TableEnumerator.enumTable(ec, 1);
        System.out.println(ec.getTableNodes().size());
        DebugHelper.printTableNodes(tns);
        /*int totalCount = 0;
        for (TableNode tn : tns) {
            totalCount += ec.export(tn, new ArrayList<>(), Arrays.asList(input).stream().map(t -> new NamedTable(t)).collect(Collectors.toList())).size();
        }*/

        /*int count = 0;
        System.out.println("tns size:" + tns.size());
        for (int i = 0; i < tns.size(); i ++) {
            for (int j = i + 1; j < tns.size(); j ++) {
                TableNode tn = tns.get(i);
                TableNode tnp = tns.get(j);
                if (tn.equals(tnp)) continue;
                Table t1 = tn.eval(new Environment());
                Table t2 = tn.eval(new Environment());
                if (t1.contentEquals(t2)) {
                    System.out.println(t1 + "\r\n" + t2);
                    System.out.println("~~~~~~~~");
                    count ++;
                }
            }
        }
        System.out.println(count);
        */
        //System.out.println(totalCount);
    }
}