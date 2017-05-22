package lang.sql.ast.abstable;

import backward_inference.MappingInference;
import lang.sql.ast.Environment;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.exception.SQLEvalException;
import lang.table.Table;
import org.junit.Test;
import util.Pair;
import util.TableExampleParser;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by clwang on 5/22/17.
 */
public class AbsAggrNodeTest {

    String src = "| id | broId | home|  datetime  | player  | resource |" + "\r\n" +
            "|----------------------------------------------------|" + "\r\n" +
            "| 1  | 1     | 10  | 04/03/2009 | john    | 399      |" + "\r\n" +
            "| 2  | 3     | 11  | 04/03/2009 | juliet  | 244      |" + "\r\n" +
            "| 5  | 4     | 12  | 04/03/2009 | borat   | 555      |" + "\r\n" +
            "| 3  | 5     | 10  | 03/03/2009 | john    | 300      |" + "\r\n" +
            "| 4  | 7     | 11  | 03/03/2009 | juliet  | 200      |" + "\r\n" +
            "| 1  | 6     | 11  | 03/03/2009 | borat   | 300      |" + "\r\n" +
            "| 7  | 5     | 13  | 24/12/2008 | borat   | 600      |" + "\r\n" +
            "| 8  | 8     | 13  | 01/01/2009 | borat   | 700      |";

    Table t1 = TableExampleParser.parseMarkDownTable("table1", src);

    String outSrc = "|id | home | resource |" + "\r\n" +
            "|-----------------|" + "\r\n" +
            "| 6 | 11   | 1300      |" + "\r\n" +
            "| 8 | 13   | 699      |";

    Table output= TableExampleParser.parseMarkDownTable("tout", outSrc);


    @Test
    public void Test4() {
        AbsTableNode agrNode = new AbsProjNode(new AbsJoinNode(new AbsNamedTable(t1), new AbsAggrNode(
                new AbsNamedTable(t1),
                Arrays.asList("table1.home"),
                Arrays.asList(new Pair<>("table1.resource", AbsAggrNode.AggrSum))
        )));

        try {
            Table result = agrNode.eval(new Environment());
            System.out.println(result);
            System.out.println(result.eliminateLazyVal());
            System.out.println(result.eliminateLazyVal().eliminateCombinedVal());

            MappingInference mi = MappingInference.buildMapping(result, output);
            System.out.println(mi);
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }

    public void Test3() {
        AbsAggrNode agrNode = new AbsAggrNode(
                new AbsNamedTable(t1),
                Arrays.asList("table1.home"),
                Arrays.asList(new Pair<>("table1.resource", AbsAggrNode.AggrMax))
        );

        try {
            Table result = agrNode.eval(new Environment());


            System.out.println(result);
            System.out.println(result.eliminateLazyVal());
            System.out.println(result.eliminateLazyVal().eliminateCombinedVal());

        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
    }


    public void Test2() {
        AbsAggrNode agrNode = new AbsAggrNode(
                new AbsNamedTable(t1),
                Arrays.asList("table1.home", "table1.player"),
                Arrays.asList(new Pair<>("table1.resource", AbsAggrNode.AggrMax)));

        AbsSelectNode sn = new AbsSelectNode(
                t1.getQualifiedMetadata().stream().map(t -> new NamedVal(t)).collect(Collectors.toList()),
                new AbsNamedTable(t1));

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