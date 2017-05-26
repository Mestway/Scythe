package backward_inference;

import lang.table.Table;
import org.junit.Test;
import util.TableExampleParser;

import static org.junit.Assert.*;

/**
 * Created by clwang on 5/24/17.
 */
public class MappingInferenceExpTest {

    @Test
    public void newTest() {
        String inputSrc =
                "| col1 | col2 | col3 | col4 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  1   |  3   | 3 |  D   |" + "\r\n" +
                        "|  2   |  1   | 1 |  B   |" + "\r\n" +
                        "|  1   |  2   | NULL[num] | NULL[str] |";

        String outputSrc =
                "| col1 | col2 | col3 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  1   |  3   |  D   |" + "\r\n" +
                        "|  2   |  1   |  B   |" + "\r\n" +
                        "|  1   |  2   | NULL[str] |";

        String outputSrc2 =
                "| col1 | col2 | col3 | col4 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  3   |  -1  |  B   | 2 |" + "\r\n" +
                        "|  4   |  2   |  D   | 1 |" + "\r\n" +
                        "|  3   |  1   | 1    | 1 |";

        Table input = TableExampleParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableExampleParser.parseMarkDownTable("table2", outputSrc);
        Table output2 = TableExampleParser.parseMarkDownTable("table22", outputSrc2);

        System.out.println(output2.contentEquals(output));

        MappingInferenceExp mi = MappingInferenceExp.buildMapping(input, output2);
        System.out.println(mi);
    }

    @Test
    public void Test1() {
        String inputSrc =
                "| col1 | col2 | col3 | col4 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  1   |  3   | 3 |  D   |" + "\r\n" +
                        "|  2   |  1   | 1 |  B   |" + "\r\n" +
                        "|  1   |  2   | 1 |  E |";

        String outputSrc =
                "| col1 | col2 |" + "\r\n" +
                        "|-------------|" + "\r\n" +
                        "|  1   |  2   |";

        Table input = TableExampleParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableExampleParser.parseMarkDownTable("table2", outputSrc);

        MappingInferenceExp mi = MappingInferenceExp.buildMapping(input, output);
        System.out.println(mi);
    }

    @Test
    public void Test2() {

        String inputSrc = "| c1 | c2 | c3 | c4 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  A   |  1993   | 1990 |  X   |" + "\r\n" +
                        "|  B   |  2011   | 2005 |  Y   |" + "\r\n" +
                        "|  C   |  2003   | 2002 | NULL[str] |";

        String outputSrc =
                "| c1 | c23 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  A   |  3    |" + "\r\n" +
                        "|  B   |  6    |" + "\r\n" +
                        "|  C   |  1  |";

        Table input = TableExampleParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableExampleParser.parseMarkDownTable("table2", outputSrc);

        System.out.println(output.contentEquals(output));

        MappingInferenceExp mi = MappingInferenceExp.buildMapping(input, output);
        System.out.println(mi);
    }

}