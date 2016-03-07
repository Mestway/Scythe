package constraint.lang.cval;

import constraint.lang.exp.CCompare;
import constraint.lang.exp.CMapsTo;
import constraint.lang.exp.CNotExp;
import constraint.lang.exp.ConstraintExp;
import constraint.lang.filter.*;
import enumerator.Constraint;
import org.junit.Assert;
import org.junit.Test;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.val.ValNode;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by clwang on 3/7/16.
 */
public class ConstraintValTest {

    @Test
    public void test1() {
        String inputSrc =
                "| id   |  rev   |  content  |" + "\r\n" +
                        "|---------------------------|" + "\r\n" +
                        "| 1    |  1     |  A        |" + "\r\n" +
                        "| 2    |  1     |  B        |" + "\r\n" +
                        "| 1    |  2     |  C        |" + "\r\n" +
                        "| 1    |  3     |  D        |";

        String outputSrc =
                "| c1 | c2 | c3 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  1   |  3   |  D   |" + "\r\n" +
                        "|  2   |  1   |  B   |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        ConstraintExp ce = new CMapsTo(Arrays.asList("c1", "c2", "c3"), Arrays.asList("id", "rev", "content"));
        Assert.assertTrue(output.getContent().stream().map(outTr -> ce.eval(outTr, input)).reduce(true, (x, y)-> (x && y))
        );

        ConstraintExp ce2 = new CCompare(
                new CNamedVal("c2"),
                new CAggrVal(AggregationNode.AggrMax,
                                "rev",
                                new CFilter(new CBiComparator(new OutParameter("c1"), new InParameter("id"), VVComparator.eq
                                ))),
                VVComparator.eq);

        Assert.assertTrue(output.getContent().stream().map(outTr -> ce2.eval(outTr, input)).reduce(true, (x,y)->(x && y)));
    }

    @Test
    public void test2() {

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
                "| c1 |    c2                  | c3 |" + "\r\n" +
                "|---------------------------------------|" + "\r\n" +
                "| 100   |   2009-02-25 10:00:00   | 15  |" + "\r\n" +
                "| 200   |   2009-02-25 10:00:00   | 20  |" + "\r\n" +
                "| 300   |   2009-02-25 10:00:00   | 24  |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        ConstraintExp ce = new CMapsTo(Arrays.asList("c1", "c2", "c3"), Arrays.asList("locId", "dtg", "temp"));
        Assert.assertTrue(output.getContent().stream().map(outTr -> ce.eval(outTr, input)).reduce(true, (x, y)-> (x && y))
        );

        ConstraintExp ce2 = new CCompare(
                new CNamedVal("c2"),
                new CAggrVal(AggregationNode.AggrMax,
                        "dtg",
                        new CFilter(new CBiComparator(new OutParameter("c1"), new InParameter("locId"), VVComparator.eq
                        ))),
                VVComparator.eq);

        Assert.assertTrue(output.getContent().stream().map(outTr -> ce2.eval(outTr, input)).reduce(true, (x,y)->(x && y)));
    }

    @Test
    public void test3() {
        String inputSrc =
        "| id | conversation_id   | name  | timestamp    |   content    |" + "\r\n" +
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

        ConstraintExp ce = new CMapsTo(
                Arrays.asList("c1", "c2", "c3", "c4", "c5"),
                Arrays.asList("id", "conversation_id", "name", "timestamp", "content"));
        Assert.assertTrue(output.getContent().stream().map(outTr -> ce.eval(outTr, input)).reduce(true, (x, y)-> (x && y)));

        ConstraintExp ce2 = new CNotExp(new CCompare(new CNamedVal("c3"), new CConstVal(Value.parse("me")), VVComparator.eq));
        Assert.assertTrue(output.getContent().stream().map(outTr -> ce2.eval(outTr, input)).reduce(true, (x, y)-> (x && y)));

        ConstraintExp ce3 = new CCompare(
                new CNamedVal("c4"),
                new CAggrVal(AggregationNode.AggrMax,
                        "timestamp",
                        new CFilter(
                                new CBiComparator(new OutParameter("c2"), new InParameter("conversation_id"), VVComparator.eq),
                                new CBiComparator(new InParameter("name"), new ConstParameter(Value.parse("me")), VVComparator.neq)
                        )),
                VVComparator.eq);
        Assert.assertTrue(output.getContent().stream().map(outTr -> ce3.eval(outTr, input)).reduce(true, (x, y)-> (x && y)));

        ConstraintExp ce4 = new CCompare(
                new CNamedVal("c1"),
                new CAggrVal(AggregationNode.AggrMax,
                        "id",
                        new CFilter(
                                new CBiComparator(new OutParameter("c2"), new InParameter("conversation_id"), VVComparator.eq),
                                new CBiComparator(new OutParameter("c4"), new InParameter("timestamp"), VVComparator.eq)
                        )),
                VVComparator.eq);
        Assert.assertTrue(output.getContent().stream().map(outTr -> ce4.eval(outTr, input)).reduce(true, (x, y)-> (x && y)));

    }

}