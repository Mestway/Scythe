package sql.lang.query;

import util.Pair;
import org.junit.Test;
import sql.lang.val.Value;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.predicate.LogicAndPred;
import sql.lang.ast.predicate.BinopPred;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.NamedTableNode;
import sql.lang.ast.table.RenameTableNode;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.TableNodeAsVal;
import util.TableExampleParser;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Case 002M in benchmark
 * Created by clwang on 12/23/15.
 */
public class QueryTest2 {
    String inputSrc =
        "| locId  |    dtg                |  temp |" + "\r\n" +
        "|----------------------------------------|" + "\r\n" +
        "| 100    |  2009-02-25 10:00:00  |  15   |" + "\r\n" +
        "| 200    |  2009-02-25 10:00:00  |  20   |" + "\r\n" +
        "| 300    |  2009-02-25 10:00:00  |  24   |" + "\r\n" +
        "| 100    |  2009-02-25 09:45:00  |  13   |" + "\r\n" +
        "| 300    |  2009-02-25 09:45:00  |  16   |" + "\r\n" +
        "| 200    |  2009-02-25 09:45:00  |  18   |" + "\r\n" +
        "| 400    |  2009-02-25 09:45:00  |  12   |" + "\r\n" +
        "| 100    |  2009-02-25 09:30:00  |  11   |" + "\r\n" +
        "| 300    |  2009-02-25 09:30:00  |  14   |" + "\r\n" +
        "| 200    |  2009-02-25 09:30:00  |  15   |" + "\r\n" +
        "| 400    |  2009-02-25 09:30:00  |  10   |";

    String outputSrc =
        "| locID |    dtg                  | tmp |" + "\r\n" +
        "|---------------------------------------|" + "\r\n" +
        "| 100   |   2009-02-25 10:00:00   | 15  |" + "\r\n" +
        "| 200   |   2009-02-25 10:00:00   | 20  |" + "\r\n" +
        "| 300   |   2009-02-25 10:00:00   | 24  |";

    Table input = TableExampleParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableExampleParser.parseMarkDownTable("table2", outputSrc);

    @Test
    public void test() {

        SQLQuery query = new SQLQuery(
            new SelectNode(
                Arrays.asList(
                    new NamedVal("table1.locId"),
                    new NamedVal("table1.dtg"),
                    new NamedVal("table1.temp")
                ),
                new NamedTableNode(input),
                new BinopPred(
                    Arrays.asList(
                        new NamedVal("table1.dtg"),
                        new TableNodeAsVal(
                            new SelectNode(
                                Arrays.asList(new NamedVal("t2.maxdtg")),
                                new RenameTableNode(
                                    "t2",
                                    Arrays.asList("id", "maxdtg"),
                                    new AggregationNode(

                                        new RenameTableNode("agrTable", new NamedTableNode(input).getSchema(),
                                                new NamedTableNode(input)),
                                        Arrays.asList("agrTable.locId"),
                                        Arrays.asList(new Pair<>("agrTable.dtg",AggregationNode.AggrMax))
                                    )
                                ),
                                new LogicAndPred(
                                    new BinopPred(
                                        Arrays.asList(
                                            new NamedVal("table1.locId"),
                                            new NamedVal("t2.id")
                                        ),
                                        BinopPred.eq
                                    ),
                                    new BinopPred(
                                        Arrays.asList(
                                            new NamedVal("t2.maxdtg"),
                                            new ConstantVal(Value.parse("2009-02-25 09:50:00"))
                                        ),
                                        BinopPred.ge
                                    )
                                )

                            ),
                            "maxdtg"
                        )
                    ),
                    BinopPred.eq
                )
            )
        );

        System.out.println(query.toString());

        assertTrue(query.execute().contentEquals(output));
    }

}
