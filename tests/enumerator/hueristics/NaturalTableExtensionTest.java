package enumerator.hueristics;

import enumerator.Constraint;
import enumerator.context.EnumContext;
import enumerator.tableenumerator.hueristics.TableNaturalJoinWithAggr;
import org.junit.Test;
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
public class NaturalTableExtensionTest {

    @Test
    public void testNaturalJoinWithAggregation() throws Exception {
        String tableSrc =
                "| id   |  rev   |  content  |" + "\r\n" +
                        "|---------------------------|" + "\r\n" +
                        "| 1    |  1     |  A        |" + "\r\n" +
                        "| 2    |  1     |  B        |" + "\r\n" +
                        "| 1    |  2     |  C        |" + "\r\n" +
                        "| 1    |  3     |  D        |";


        String tableSrc2 =
                "| user_id |  names |" + "\r\n" +
                "|------------------|" + "\r\n" +
                "| 701     |  Name1 |" + "\r\n" +
                "| 701     |  Name2 |" + "\r\n" +
                "| 701     |  Name3 |" + "\r\n" +
                "| 701     |  Name4 |" + "\r\n" +
                "| 702     |  Name5 |" + "\r\n" +
                "| 702     |  Name6 |" + "\r\n" +
                "| 703     |  Name7 |" + "\r\n" +
                "| 703     |  Name8 |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", tableSrc2);

        Constraint c = new Constraint(3, new ArrayList<>(), Arrays.asList(AggregationNode.AggrCount), 1);

        List<TableNode> tns = TableNaturalJoinWithAggr.naturalJoinWithAggregation(new EnumContext(Arrays.asList(input), c));

        //DebugHelper.printTableNodes(tns);

        for (TableNode tn : tns) {
            System.out.println(tn.prettyPrint(0));
            DebugHelper.printList(tn.getSchema());
            DebugHelper.printList(tn.getSchemaType());
        }
    }
}