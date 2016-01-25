package enumerator;

import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/8/16.
 */
public class EnumTest13 {

    // I need to select each distinct home holding the maximum value of datetime.
    
    String inputSrc =
    "|  a  | b |" + "\r\n" +
            "|---------|" + "\r\n" +
            "| ALA | 2 |" + "\r\n" +
            "| ASP | 1 |" + "\r\n" +
            "| SER | 1 |" + "\r\n" +
            "| VAL | 2 |";


    String outputSrc =  "|  a  | max(b) |" + "\r\n" +
            "|--------------|" + "\r\n" +
            "| ALA |   2    |" + "\r\n" +
            "| VAL |   2    |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrMax),
            2);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(Arrays.asList(input), output, c);
        DebugHelper.printTableNodes(tn);
    }
}
