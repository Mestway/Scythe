package sql.lang.query;

import util.Pair;
import org.junit.Test;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.TableAsVal;
import util.TableInstanceParser;

import java.lang.reflect.Array;
import java.util.Arrays;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Created by clwang on 1/4/16.
 */
public class QueryTest4 {

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

    @Test
    public void test() {
        SQLQuery query = new SQLQuery(
            new SelectNode(
                Arrays.asList(
                    new NamedVal("table1.id"),
                    new NamedVal("table1.customer"),
                    new NamedVal("table1.total")
                ),
                new NamedTable(input),
                new VVComparator(
                    Arrays.asList(
                        new NamedVal("table1.total"),
                        new TableAsVal(
                            new SelectNode(
                                Arrays.asList(new NamedVal("tt.total")),
                                new RenameTableNode(
                                    "tt",
                                    Arrays.asList("customer", "total"),
                                    new AggregationNode(
                                        new RenameTableNode(
                                            "t1",
                                            new NamedTable(input)
                                        ),
                                        Arrays.asList("t1.customer"),
                                        Arrays.asList(new Pair<>("t1.total", AggregationNode.AggrMax))
                                    )
                                ),
                                new VVComparator(
                                    Arrays.asList(
                                        new NamedVal("table1.customer"),
                                        new NamedVal("tt.customer")
                                    ),
                                    VVComparator.eq
                                )
                            ), "v"
                        )
                    ),
                    VVComparator.eq
                )
            )
        );

        assertTrue(query.execute().contentEquals(output));
    }

}
