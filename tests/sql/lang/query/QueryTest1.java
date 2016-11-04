package sql.lang.query;

import util.Pair;
import org.junit.Test;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.RenameTableNode;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.TableAsVal;
import util.TableInstanceParser;

import java.util.Arrays;
import java.util.concurrent.SynchronousQueue;

import static org.junit.Assert.assertTrue;

/**
 * Case 001R in the benchmark
 * Created by clwang on 12/23/15.
 */
public class QueryTest1 {

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

    Table inputTable = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table t2 = TableInstanceParser.parseMarkDownTable("output", outputSrc);

    @Test
    public void test() {

        /*
        *  SELECT table1.id, table1.rev, table1.content
        *  FROM table1
        *  WHERE table1.rev = (
        *     SELECT t22.maxrev
        *     FROM (
        *        SELECT id, max(rev) as maxrev
        *        FROM table1 as t2
        *        GROUP BY id
        *     ) as t22
        *     WHERE table1.id = t22.id
        *  )
        * */
        SQLQuery query = new SQLQuery(
            new SelectNode(
                Arrays.asList(
                    new NamedVal("table1.id"),
                    new NamedVal("table1.rev"),
                    new NamedVal("table1.content")),
                new NamedTable(inputTable),
                new VVComparator(
                    Arrays.asList(
                        new NamedVal("table1.rev"),
                        new TableAsVal(
                            new SelectNode(
                                Arrays.asList(new NamedVal("t22.maxrev")),
                                new RenameTableNode(
                                    "t22",
                                    Arrays.asList("id", "maxrev"),
                                    new AggregationNode(
                                        new RenameTableNode("t2",new NamedTable(inputTable)),
                                        Arrays.asList("t2.id"),
                                        Arrays.asList(new Pair<>("t2.rev", AggregationNode.AggrMax))
                                    )
                                ),
                                new VVComparator(
                                    Arrays.asList(
                                        new NamedVal("table1.id"),
                                        new NamedVal("t22.id")
                                    ),
                                    VVComparator.eq
                                )
                            ), "maxrev"
                        )
                    ),
                    VVComparator.eq
                )
            )
        );

        System.out.println(query.toString());

        assertTrue(query.execute().contentEquals(t2));
    }
}
