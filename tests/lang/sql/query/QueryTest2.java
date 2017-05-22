package lang.sql.query;

import lang.sql.SQLQuery;
import lang.table.Table;
import lang.sql.ast.predicate.BinopPred;
import lang.sql.ast.predicate.LogicAndPred;
import lang.sql.ast.contable.AggregationNode;
import lang.sql.ast.contable.NamedTableNode;
import lang.sql.ast.contable.RenameTableNode;
import lang.sql.ast.contable.SelectNode;
import lang.sql.ast.valnode.ConstantVal;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.TableNodeAsVal;
import lang.sql.dataval.Value;
import util.Pair;
import org.junit.Test;
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
