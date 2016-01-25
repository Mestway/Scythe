package sql.lang.query;

import javafx.util.Pair;
import org.junit.Test;
import sql.lang.DataType.StringVal;
import sql.lang.DataType.Value;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.filter.LogicNegFilter;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.*;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.NamedVal;
import util.TableInstanceParser;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Created by clwang on 1/5/16.
 */
public class QueryTest8 {
    String inputSrc =
        "| message_id | conversation_id | from_user | timestamp  |  message    |" + "\r\n" +
        "|-----------------------------------|" + "\r\n" +
        "| 2  | 145  | xxx | 10000 | message |" + "\r\n" +
        "| 6  | 1743 | yyy | 999   | message |" + "\r\n" +
        "| 7  | 14   | bbb | 899   | message |" + "\r\n" +
        "| 1  | 145  | xxx | 10000 | message |" + "\r\n" +
        "| 5  | 1743 | me  | 1200  | message |";


    String outputSrc =
        "| c1 | c2   |  c3 | c4    |  c5     |" + "\r\n" +
        "|-----------------------------------|" + "\r\n" +
        "| 2  | 145  | xxx | 10000 | message |" + "\r\n" +
        "| 6  | 1743 | yyy | 999   | message |" + "\r\n" +
        "| 7  | 14   | bbb | 899   | message |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    @Test
    public void test() {

        TableNode t1 =
            new RenameTableNode(
                "t1",
                Arrays.asList("message_id" , "conversation_id", "from_user", "timestamp", "message"),
                new SelectNode(
                    Arrays.asList(
                        new NamedVal("table1.message_id"),
                        new NamedVal("table1.conversation_id"),
                        new NamedVal("table1.from_user"),
                        new NamedVal("table1.timestamp"),
                        new NamedVal("table1.message")
                    ),
                    new NamedTable(input),
                    new LogicNegFilter(
                        new VVComparator(
                            Arrays.asList(
                                new NamedVal("table1.from_user"),
                                new ConstantVal(new StringVal("me"))
                            ),
                            VVComparator.eq
                        )
                    )
                )
            );

        TableNode t2 =
            new RenameTableNode(
                "t2",
                Arrays.asList("conversation_id", "max_timestamp"),
                new AggregationNode(
                    t1,
                    Arrays.asList("t1.conversation_id"),
                    Arrays.asList(new Pair<>("t1.timestamp",AggregationNode.AggrMax))
                )
            );

        TableNode t3 =
            new RenameTableNode(
                "t3",
                Arrays.asList("message_id" , "conversation_id", "from_user", "timestamp", "message"),
                new SelectNode(
                    Arrays.asList(
                        new NamedVal("t1.message_id"),
                        new NamedVal("t1.conversation_id"),
                        new NamedVal("t1.from_user"),
                        new NamedVal("t1.timestamp"),
                        new NamedVal("t1.message")
                    ),
                    new JoinNode(
                        Arrays.asList(
                            t2,
                            t1
                        )
                    ),
                    new LogicAndFilter(
                        new VVComparator(
                            Arrays.asList(
                                new NamedVal("t1.conversation_id"),
                                new NamedVal("t2.conversation_id")
                            ),
                            VVComparator.eq
                        ),
                        new VVComparator(
                            Arrays.asList(
                                new NamedVal("t1.timestamp"),
                                new NamedVal("t2.max_timestamp")
                            ),
                            VVComparator.eq
                        )
                    )
                )
            );

        TableNode t4 =
            new RenameTableNode(
                "t4",
                Arrays.asList("conversation_id", "max_message_id"),
                new AggregationNode(
                    t1,
                    Arrays.asList("t3.conversation_id"),
                    Arrays.asList(new Pair<>("t3.message_id", AggregationNode.AggrMax))
                )
            );

        TableNode t5 =
            new RenameTableNode(
                "t5",
                Arrays.asList("message_id" , "conversation_id", "from_user", "timestamp", "message"),
                new SelectNode(
                    Arrays.asList(
                        new NamedVal("t3.message_id"),
                        new NamedVal("t3.conversation_id"),
                        new NamedVal("t3.from_user"),
                        new NamedVal("t3.timestamp"),
                        new NamedVal("t3.message")
                    ),
                    new JoinNode(
                        Arrays.asList(
                            t4,
                            t3
                        )
                    ),
                    new LogicAndFilter(
                        new VVComparator(
                            Arrays.asList(
                                new NamedVal("t3.conversation_id"),
                                new NamedVal("t4.conversation_id")
                            ),
                            VVComparator.eq
                        ),
                        new VVComparator(
                            Arrays.asList(
                                new NamedVal("t3.message_id"),
                                new NamedVal("t4.max_message_id")
                            ),
                            VVComparator.eq
                        )
                    )
                )
            );

        SQLQuery query = new SQLQuery(t5);
        assertTrue(query.execute().contentEquals(output));
    }

}
