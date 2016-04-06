package sql.lang.query;

import util.Pair;
import org.junit.Test;
import sql.lang.DataType.NumberVal;
import sql.lang.SQLQuery;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.*;
import sql.lang.ast.val.AsNamedVal;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.TableAsVal;
import sql.lang.exception.SQLEvalException;
import util.TableInstanceParser;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test cases for the benchmark
 * Created by clwang on 12/14/15.
 */
public class SQLQueryTest {

    String src =
            "| id | broId | home|  datetime  | player  | resource |" + "\r\n" +
            "|----------------------------------------------------|" + "\r\n" +
            "| 1  | 1     | 10  | 04/03/2009 | john    | 399      |" + "\r\n" +
            "| 2  | 3     | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
            "| 5  | 4     | 12  | 04/03/2009 | borat   | 555      |" + "\r\n" +
            "| 3  | 5     | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
            "| 4  | 7     | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n" +
            "| 6  | 6     | 12  | 03/03/2009 | borat   | 500      |" + "\r\n" +
            "| 7  | 5     | 13  | 24/12/2008 | borat   | 600      |" + "\r\n" +
            "| 8  | 8     | 13  | 01/01/2009 | borat   | 700      |";

    Table testTable = TableInstanceParser.parseMarkDownTable("input", src);

    @Test
    public void testSelect1() throws Exception {
        /**
         * SELECT input.id, input.broId, input.datetime, input.player
         * FROM input
         * WHERE input.id < input.broId
         */
        SQLQuery query = new SQLQuery(
                new SelectNode(
                        Arrays.asList(
                                new NamedVal("input.id"),
                                new NamedVal("input.broId"),
                                new NamedVal("input.home"),
                                new NamedVal("input.datetime"),
                                new NamedVal("input.player"),
                                new NamedVal("input.resource")),
                        new NamedTable(testTable),
                        new VVComparator(
                                Arrays.asList(new NamedVal("input.id"), new NamedVal("input.broId")),VVComparator.lt)
                )
        );

        String result1 =
                "| input.id | input.broId | input.home|  input.datetime  | input.player  | input.resource |" + "\r\n" +
                "|----------------------------------------------------|" + "\r\n" +
                "| 2  | 3     | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
                "| 3  | 5     | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
                "| 4  | 7     | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n";

        Table resultTable1 = TableInstanceParser.parseMarkDownTable("result1", result1);

        assertTrue(query.execute().tableEquals(resultTable1));
    }

    @Test
    public void testSelect2() throws Exception {
        /**
         * SELECT input.id, input.broId, input.datetime, input.player
         * FROM input
         * WHERE input.resource <= 300
         */
        SQLQuery query2 = new SQLQuery(
                new SelectNode(
                        Arrays.asList(
                                new NamedVal("input.id"),
                                new NamedVal("input.broId"),
                                new NamedVal("input.datetime"),
                                new NamedVal("input.player")),
                        new NamedTable(testTable),
                        new VVComparator(Arrays.asList(new NamedVal("input.resource"), new ConstantVal(new NumberVal(300))),VVComparator.le)
                )
        );

        String result2 =
                "| input.id | input.broId |  input.datetime  | input.player  |" + "\r\n" +
                        "|-----------------------------------|" + "\r\n" +
                        "| 2  | 3     | 04/03/2009 | juliet  |" + "\r\n" +
                        "| 3  | 5     | 03/03/2009 | john    |" + "\r\n" +
                        "| 4  | 7     | 03/03/2009 | juliet  |";
        Table resultTable2 = TableInstanceParser.parseMarkDownTable("resultTable", result2);

        assertTrue(query2.execute().contentEquals(resultTable2));
    }

    @Test
    public void testSelect3() throws Exception {
        /**
         * SELECT input.id, 30 AS nm
         * FROM input
         * WHERE input.resource <= 300
         */
        SQLQuery query = new SQLQuery(
                new SelectNode(
                        Arrays.asList(
                                new NamedVal("input.id"),
                                new AsNamedVal(new ConstantVal(new NumberVal(30)), "nm")),
                        new NamedTable(testTable),
                        new VVComparator(Arrays.asList(new NamedVal("input.resource"), new ConstantVal(new NumberVal(300))),VVComparator.le)
                )
        );

        String result =
                "| input.id | nm  |" + "\r\n" +
                "|---------|" + "\r\n" +
                "| 2  | 30 |" + "\r\n" +
                "| 3  | 30 |" + "\r\n" +
                "| 4  | 30 |";
        Table resultTable = TableInstanceParser.parseMarkDownTable("resultTable", result);

        assertTrue(query.execute().contentEquals(resultTable));
    }


    @Test
    public void testSelectJoin() {

        String src =
                "| id | broId | home|  datetime  | player  | resource |" + "\r\n" +
                        "|----------------------------------------------------|" + "\r\n" +
                        "| 1  | 1     | 10  | 04/03/2009 | john    | 399      |" + "\r\n" +
                        "| 2  | 3     | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
                        "| 5  | 4     | 12  | 04/03/2009 | borat   | 555      |" + "\r\n" +
                        "| 3  | 5     | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
                        "| 4  | 7     | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n" +
                        "| 6  | 6     | 12  | 03/03/2009 | borat   | 500      |" + "\r\n" +
                        "| 7  | 5     | 13  | 24/12/2008 | borat   | 600      |" + "\r\n" +
                        "| 8  | 8     | 13  | 01/01/2009 | borat   | 700      |";

        String src2 =
                "| home | maxresource   |" + "\r\n" +
                "|------------|" + "\r\n" +
                "| 10  | 399  |" + "\r\n" +
                "| 11  | 244  |" + "\r\n" +
                "| 12  | 555  |" + "\r\n" +
                "| 13  | 700  |";
        Table t1 = TableInstanceParser.parseMarkDownTable("table1", src);
        Table t2 = TableInstanceParser.parseMarkDownTable("table2", src2);


        try {Table t = new JoinNode(
                Arrays.asList(
                        new NamedTable(t1),
                        new NamedTable(t2))).eval(new Environment());
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }

        SQLQuery query = new SQLQuery(
            new SelectNode(
                    Arrays.asList(
                            new NamedVal("table1.id"),
                            new NamedVal("table1.home"),
                            new NamedVal("table2.maxresource")),
                    new JoinNode(
                            Arrays.asList(
                            new NamedTable(t1),
                            new NamedTable(t2))),
                    new LogicAndFilter(
                            new VVComparator(
                                    Arrays.asList(
                                            new NamedVal("table1.home"),
                                            new NamedVal("table2.home"))
                                    ,VVComparator.eq),
                            new VVComparator(
                                    Arrays.asList(
                                            new NamedVal("table1.resource"),
                                            new NamedVal("table2.maxresource")
                                    ),
                                    VVComparator.eq)
                    )
            )
        );

        Table exeResult = query.execute();

        String resultSrc =
                "|id | datetime | maxresource   |" + "\r\n" +
                "|--------------|" + "\r\n" +
                "|1| 10  | 399  |" + "\r\n" +
                "|2| 11  | 244  |" + "\r\n" +
                "|5| 12  | 555  |" + "\r\n" +
                "|8| 13  | 700  |";

        assertTrue(TableInstanceParser.parseMarkDownTable("anonymous", resultSrc).contentEquals(exeResult));
    }


    @Test
    public void testSubQuery() {

        String src =
                "| id | broId | home|  datetime  | player  | resource |" + "\r\n" +
                        "|----------------------------------------------------|" + "\r\n" +
                        "| 1  | 1     | 10  | 04/03/2009 | john    | 399      |" + "\r\n" +
                        "| 2  | 3     | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
                        "| 5  | 4     | 12  | 04/03/2009 | borat   | 555      |" + "\r\n" +
                        "| 3  | 5     | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
                        "| 4  | 7     | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n" +
                        "| 6  | 6     | 12  | 03/03/2009 | borat   | 500      |" + "\r\n" +
                        "| 7  | 5     | 13  | 24/12/2008 | borat   | 600      |" + "\r\n" +
                        "| 8  | 8     | 13  | 01/01/2009 | borat   | 700      |";

        String src2 =
                "| home | maxresource   |" + "\r\n" +
                "|------------|" + "\r\n" +
                "| 10  | 399  |" + "\r\n" +
                "| 11  | 244  |" + "\r\n" +
                "| 12  | 555  |" + "\r\n" +
                "| 13  | 700  |";

        Table t1 = TableInstanceParser.parseMarkDownTable("table1", src);
        Table t2 = TableInstanceParser.parseMarkDownTable("table2", src2);

        SQLQuery q0 = new SQLQuery(
                new SelectNode(
                        Arrays.asList(
                                new NamedVal("table1.id"),
                                new NamedVal("table1.home"),
                                new NamedVal("table1.resource")),
                        new NamedTable(t1),
                        new VVComparator(
                                Arrays.asList(
                                        new NamedVal("table1.resource"),
                                        new TableAsVal(
                                                new SelectNode(
                                                        Arrays.asList(new NamedVal("table2.maxresource")),
                                                        new NamedTable(t2),
                                                        new VVComparator(
                                                                Arrays.asList(
                                                                        new NamedVal("table2.home"),
                                                                        new NamedVal("table1.home")
                                                                ),
                                                                VVComparator.eq
                                                        )
                                                ),
                                                "max-resource")
                                ),
                                VVComparator.eq
                        )
                )
        );

        Table exeResult = q0.execute();

        String resultSrc =
                "|id | datetime | maxresource   |" + "\r\n" +
                "|--------------|" + "\r\n" +
                "|1| 10  | 399  |" + "\r\n" +
                "|2| 11  | 244  |" + "\r\n" +
                "|5| 12  | 555  |" + "\r\n" +
                "|8| 13  | 700  |";

        assertTrue(TableInstanceParser.parseMarkDownTable("anonymous", resultSrc).contentEquals(exeResult));
    }

    @Test
    public void testAggregation() {

        String src =
                "| id | broId | home|  datetime  | player  | resource |" + "\r\n" +
                "|----------------------------------------------------|" + "\r\n" +
                "| 1  | 1     | 10  | 04/03/2009 | john    | 399      |" + "\r\n" +
                "| 2  | 3     | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
                "| 5  | 4     | 12  | 04/03/2009 | borat   | 555      |" + "\r\n" +
                "| 3  | 5     | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
                "| 4  | 7     | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n" +
                "| 6  | 6     | 12  | 03/03/2009 | borat   | 500      |" + "\r\n" +
                "| 7  | 5     | 13  | 24/12/2008 | borat   | 600      |" + "\r\n" +
                "| 8  | 8     | 13  | 01/01/2009 | borat   | 700      |";

        String src2 =
                "| home | maxresource   |" + "\r\n" +
                "|------------|" + "\r\n" +
                "| 10  | 399  |" + "\r\n" +
                "| 11  | 244  |" + "\r\n" +
                "| 12  | 555  |" + "\r\n" +
                "| 13  | 700  |";

        Table t1 = TableInstanceParser.parseMarkDownTable("table1", src);
        //Table t2 = TableInstanceParser.parseMarkDownTable("table2", src2);

        Table t2 = new SQLQuery(
            new RenameTableNode(
                "agrResult",
                Arrays.asList("home", "maxresource"),
                new AggregationNode(
                    new NamedTable(t1),
                    Arrays.asList("table1.home"),
                    Arrays.asList(new Pair<>("table1.resource", AggregationNode.AggrMax))
                )
            )
        ).execute();

        assertTrue(TableInstanceParser.parseMarkDownTable("table2", src2).contentEquals(t2));

        SQLQuery q0 = new SQLQuery(
            new SelectNode(
                Arrays.asList(
                    new NamedVal("table1.id"),
                    new NamedVal("table1.home"),
                    new NamedVal("table1.resource")),
                new NamedTable(t1),
                new VVComparator(
                    Arrays.asList(
                        new NamedVal("table1.resource"),
                        new TableAsVal(
                            new SelectNode(
                                Arrays.asList(new NamedVal("agrResult.maxresource")),
                                new NamedTable(t2),
                                new VVComparator(
                                    Arrays.asList(
                                            new NamedVal("agrResult.home"),
                                            new NamedVal("table1.home")
                                    ),
                                    VVComparator.eq
                                )
                            ),
                            "max-resource"
                        )
                    ),
                    VVComparator.eq
                )
            )
        );

        Table exeResult = q0.execute();

        String resultSrc =
                "|id | datetime | maxresource   |" + "\r\n" +
                "|----------------|" + "\r\n" +
                "| 1 | 10  | 399  |" + "\r\n" +
                "| 2 | 11  | 244  |" + "\r\n" +
                "| 5 | 12  | 555  |" + "\r\n" +
                "| 8 | 13  | 700  |";

        assertTrue(TableInstanceParser.parseMarkDownTable("anonymous", resultSrc).contentEquals(exeResult));
    }

}