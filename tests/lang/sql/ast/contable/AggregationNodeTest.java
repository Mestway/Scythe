package lang.sql.ast.contable;

import lang.table.Table;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.exception.SQLEvalException;
import lang.sql.ast.predicate.BinopPred;
import util.Pair;
import org.junit.Test;
import lang.sql.ast.Environment;
import util.TableExampleParser;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/22/15.
 */
public class AggregationNodeTest {
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

    Table t1 = TableExampleParser.parseMarkDownTable("table1", src);

    public void Test1() {
        AggregationNode agrNode = new AggregationNode(
                new NamedTableNode(t1),
                Arrays.asList("table1.home"),
                Arrays.asList(new Pair<>("table1.resource", AggregationNode.AggrMax))
        );

        try {
            System.out.println(agrNode.eval(new Environment()));
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void Test2() {
        AggregationNode agrNode = new AggregationNode(
                new NamedTableNode(t1),
                Arrays.asList("table1.home", "table1.player"),
                Arrays.asList(new Pair<>("table1.resource", AggregationNode.AggrMax)));

        SelectNode sn = new SelectNode(t1.getQualifiedMetadata().stream().map(t -> new NamedVal(t)).collect(Collectors.toList()),
                new NamedTableNode(t1), new BinopPred(Arrays.asList(new NamedVal("table1.id"), new NamedVal("table1.broId")), BinopPred.le));

        try {

            System.out.println(sn.prettyPrint(0, false));
            System.out.println(sn.eval(new Environment()));

            System.out.println(" - - - - - - - -  - -");

            System.out.println(agrNode.eval(new Environment()));

            System.out.println(agrNode.prettyPrint(0, false));

            System.out.println(" - - - - - - - -  - -");

            System.out.println(agrNode.substCoreTable(sn).prettyPrint(0, false));

            System.out.println(agrNode.substCoreTable(sn).eval(new Environment()));

        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }
}