package sql.lang.query;

import org.junit.Test;
import sql.lang.val.Value;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.predicate.LogicAndPred;
import sql.lang.ast.predicate.BinopPred;
import sql.lang.ast.table.NamedTableNode;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.NamedVal;
import util.TableExampleParser;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Created by clwang on 1/4/16.
 */
public class QueryTest7 {

    String inputSrc =
            "| yrq  | start_date | end_date   |" + "\r\n" +
            "|--------------------------------|" + "\r\n" +
            "| B233 | 2013-01-07 | 2013-03-23 |" + "\r\n" +
            "| B232 | 2012-09-24 | 2012-12-20 |" + "\r\n" +
            "| B231 | 2012-06-25 | 2012-09-13 |" + "\r\n" +
            "| B124 | 2012-04-02 | 2012-06-21 |" + "\r\n" +
            "| B123 | 2012-01-09 | 2012-03-27 |";

    String outputSrc =
            "| col1 |" + "\r\n" +
            "|------|" + "\r\n" +
            "| B233 |";

    Table input = TableExampleParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableExampleParser.parseMarkDownTable("table2", outputSrc);

    @Test
    public void test() {
        SQLQuery query = new SQLQuery(
            new SelectNode(
                Arrays.asList(
                    new NamedVal("table1.yrq")
                ),
                new NamedTableNode(input),
                new LogicAndPred(
                    new BinopPred(
                        Arrays.asList(
                            new NamedVal("table1.start_date"),
                            new ConstantVal(Value.parse("2013-02-01"))
                        ),
                        BinopPred.le
                    ),
                    new BinopPred(
                        Arrays.asList(
                            new NamedVal("table1.end_date"),
                            new ConstantVal(Value.parse("2013-02-15"))
                        ),
                        BinopPred.ge
                    )
                )

            )
        );

        assertTrue(query.execute().contentEquals(output));
    }

}
