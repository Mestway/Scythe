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
public class QueryTest5 {

    String inputSrc =
            "| Id |  Name |  Other_Columns | \r\n" +
            "|-----------------------------| \r\n" +
            "| 1  |  A    |   A_data_1     | \r\n" +
            "| 2  |  A    |   A_data_2     | \r\n" +
            "| 3  |  A    |   A_data_3     | \r\n" +
            "| 4  |  B    |   B_data_1     | \r\n" +
            "| 5  |  B    |   B_data_2     | \r\n" +
            "| 6  |  C    |   C_data_1     |";

    String outputSrc =
            "| col1 | col2 | col3     | \r\n" +
            "|------------------------| \r\n" +
            "| 3    | A    | A_data_3 | \r\n" +
            "| 5    | B    | B_data_2 | \r\n" +
            "| 6    | C    | C_data_1 |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    @Test
    public void test() {

        SQLQuery query;
        query = new SQLQuery(
                new SelectNode(
                        Arrays.asList(
                                new NamedVal("table1.Id"),
                                new NamedVal("table1.Name"),
                                new NamedVal("table1.Other_Columns")
                        ),
                        new NamedTable(input),
                        new VVComparator(
                                Arrays.asList(
                                        new NamedVal("table1.Id"),
                                        new TableAsVal(
                                                new SelectNode(
                                                        Arrays.asList(new NamedVal("tt.MinID")),
                                                        new RenameTableNode(
                                                                "tt",
                                                                Arrays.asList("Name", "MinID"),
                                                                new AggregationNode(
                                                                        new RenameTableNode(
                                                                                "t1",
                                                                                new NamedTable(input)
                                                                        ),
                                                                        Arrays.asList("t1.Name"),
                                                                        Arrays.asList(new Pair<>("t1.Id", AggregationNode.AggrMax))
                                                                )
                                                        ),
                                                        new VVComparator(
                                                                Arrays.asList(
                                                                        new NamedVal("table1.Name"),
                                                                        new NamedVal("tt.Name")
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
