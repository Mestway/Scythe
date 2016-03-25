package enumerator.tableenumerator;

import enumerator.Constraint;
import enumerator.TableEnumerator;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by clwang on 3/21/16.
 */
public class PlainTableEnumeratorTest {
    String inputSrc =
            "| id  | customer | total |\r\n" +
                    "|------------------------|\r\n" +
                    "| 1   | Joe      | 5     |\r\n " +
                    "| 2   | Sally    | 3     |\r\n" +
                    "| 3   | Joe      | 2     |\r\n" +
                    "| 4   | Sally    | 1     |";

    String outputSrc =
            "| FIRST(id) | customer | FIRST(total) |\r\n" +
                    "|-------------------------------------|\r\n " +
                    "|         1 | Joe      | 5            |\r\n " +
                    "|         2 | Sally    | 3            |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            new ArrayList<>(),
            Arrays.asList(AggregationNode.AggrMin),
            0);

    @Test
    public void test1() {
        c.setMaxFilterLength(4);
        List<TableNode> tn = new PlainTableEnumerator().enumProgramWithIO(Arrays.asList(input), output, c);
        //DebugHelper.printTableNodes(tn);
    }
    @Test
    public void test2() {
        c.setMaxFilterLength(2);
        List<TableNode> tn = new CanonicalTableEnumerator().enumProgramWithIO(Arrays.asList(input), output, c);
        //DebugHelper.printTableNodes(tn);
    }
    @Test
    public void test3() {
        c.setMaxFilterLength(4);
        List<TableNode> tn = new AggrHueristicTableEnumerator().enumProgramWithIO(Arrays.asList(input), output, c);
        //DebugHelper.printTableNodes(tn);
    }
}