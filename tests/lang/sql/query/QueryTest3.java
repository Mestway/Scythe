package lang.sql.query;

import lang.sql.ast.contable.RenameTableNode;
import util.Pair;
import org.junit.Test;
import lang.sql.SQLQuery;
import lang.table.Table;
import lang.sql.ast.predicate.BinopPred;
import lang.sql.ast.contable.AggregationNode;
import lang.sql.ast.contable.NamedTableNode;
import lang.sql.ast.contable.SelectNode;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.TableNodeAsVal;
import util.TableExampleParser;

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

    Table input = TableExampleParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableExampleParser.parseMarkDownTable("table2", outputSrc);

    @Test
    public void test() {

        SQLQuery query = new SQLQuery(
            new SelectNode(
                Arrays.asList(
                    new NamedVal("table1.Name"),
                    new NamedVal("table1.City")
                ),
                new NamedTableNode(input),
                new BinopPred(
                    Arrays.asList(
                        new NamedVal("table1.Birthyear"),
                        new TableNodeAsVal(
                            new SelectNode(
                                Arrays.asList(new NamedVal("tt.maxBirth")),
                                new RenameTableNode(
                                    "tt",
                                    Arrays.asList("City", "maxBirth"),
                                    new AggregationNode(
                                        new RenameTableNode("t2", new NamedTableNode(input).getSchema(),
                                                new NamedTableNode(input)),
                                        Arrays.asList("t2.City"),
                                        Arrays.asList(new Pair<>("t2.Birthyear", AggregationNode.AggrMin))
                                    )
                                ),
                                new BinopPred(
                                    Arrays.asList(new NamedVal("tt.City"), new NamedVal("table1.City")),
                                    BinopPred.eq
                                )
                            ),
                            "mmax"
                        )
                    ),
                    BinopPred.eq
                )
            )
        );

        assertTrue(query.execute().containsContent(output));
    }
}
