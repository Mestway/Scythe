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

import static org.junit.Assert.assertTrue;

/**
 * Created by clwang on 1/4/16.
 */
public class QueryTest3 {
    String inputSrc =
            "| ID  | Name            | City           | Birthyear  |\r\n" +
            "|-----------------------------------------------------|\r\n" +
            "| 1   | Egon Spengler   | New York       | 1957       |\r\n" +
            "| 2   | Mac Taylor      | New York       | 1955       |\r\n" +
            "| 3   | Sarah Connor    | Los Angeles    | 1959       |\r\n" +
            "| 4   | Jean-Luc Picard | La Barre       | 2305       |\r\n" +
            "| 5   | Ellen Ripley    | Nostromo       | 2092       |\r\n" +
            "| 6   | James T. Kirk   | Riverside      | 2233       |\r\n" +
            "| 7   | Henry Jones     | Chicago        | 1899       |\r\n";

    String outputSrc =
            "| col1          |  col2       |" +
            "|-----------------------------|" +
            "| Henry Jones   | Chicago     |" +
            "| Mac Taylor    | New York    |" +
            "| Sarah Connor  | Los Angeles |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    @Test
    public void test() {

        SQLQuery query = new SQLQuery(
            new SelectNode(
                Arrays.asList(
                    new NamedVal("table1.Name"),
                    new NamedVal("table1.City")
                ),
                new NamedTable(input),
                new VVComparator(
                    Arrays.asList(
                        new NamedVal("table1.Birthyear"),
                        new TableAsVal(
                            new SelectNode(
                                Arrays.asList(new NamedVal("tt.maxBirth")),
                                new RenameTableNode(
                                    "tt",
                                    Arrays.asList("City", "maxBirth"),
                                    new AggregationNode(
                                        new RenameTableNode("t2", new NamedTable(input)),
                                        Arrays.asList("t2.City"),
                                        Arrays.asList(new Pair<>("t2.Birthyear", AggregationNode.AggrMin))
                                    )
                                ),
                                new VVComparator(
                                    Arrays.asList(new NamedVal("tt.City"), new NamedVal("table1.City")),
                                    VVComparator.eq
                                )
                            ),
                            "mmax"
                        )
                    ),
                    VVComparator.eq
                )
            )
        );

        assertTrue(query.execute().containsContent(output));
    }
}
