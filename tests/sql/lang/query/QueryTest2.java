package sql.lang.query;

import util.Pair;
import org.junit.Test;
import sql.lang.datatype.Value;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.filter.BinopFilter;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.RenameTableNode;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.TableAsVal;
import util.TableInstanceParser;

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

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    @Test
    public void test() {

        SQLQuery query = new SQLQuery(
            new SelectNode(
                Arrays.asList(
                    new NamedVal("table1.locId"),
                    new NamedVal("table1.dtg"),
                    new NamedVal("table1.temp")
                ),
                new NamedTable(input),
                new BinopFilter(
                    Arrays.asList(
                        new NamedVal("table1.dtg"),
                        new TableAsVal(
                            new SelectNode(
                                Arrays.asList(new NamedVal("t2.maxdtg")),
                                new RenameTableNode(
                                    "t2",
                                    Arrays.asList("id", "maxdtg"),
                                    new AggregationNode(

                                        new RenameTableNode("agrTable", new NamedTable(input).getSchema(),
                                                new NamedTable(input)),
                                        Arrays.asList("agrTable.locId"),
                                        Arrays.asList(new Pair<>("agrTable.dtg",AggregationNode.AggrMax))
                                    )
                                ),
                                new LogicAndFilter(
                                    new BinopFilter(
                                        Arrays.asList(
                                            new NamedVal("table1.locId"),
                                            new NamedVal("t2.id")
                                        ),
                                        BinopFilter.eq
                                    ),
                                    new BinopFilter(
                                        Arrays.asList(
                                            new NamedVal("t2.maxdtg"),
                                            new ConstantVal(Value.parse("2009-02-25 09:50:00"))
                                        ),
                                        BinopFilter.ge
                                    )
                                )

                            ),
                            "maxdtg"
                        )
                    ),
                    BinopFilter.eq
                )
            )
        );

        System.out.println(query.toString());

        assertTrue(query.execute().contentEquals(output));
    }

}
