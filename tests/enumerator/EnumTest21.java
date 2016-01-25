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
public class EnumTest21 {

    //I want a query that count the number of rows, but distinct by 3 parameters.


    String inputSrc =
            "| Id   |   Name       |  Adress   |" + "\r\n" +
            "|---------------------------------|" + "\r\n" +
            "| 1    | MyName       |  MyAdress |" + "\r\n" +
            "| 2    | MySecondName |  Adress2  |";

    String outputSrc =
            "| c |"+ "\r\n" +
            "|---|"+ "\r\n" +
            "| 2 |";

    List<Table> inputs = Arrays.asList(
            TableInstanceParser.parseMarkDownTable("table1", inputSrc));
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    Constraint c = new Constraint(
            1,
            Arrays.asList(),
            Arrays.asList(AggregationNode.AggrCount),
            2);

    @Test
    public void test() {
        List<TableNode> tn = TableEnumerator.enumProgramWithIO(inputs, output, c);
        DebugHelper.printTableNodes(tn);
    }
}
