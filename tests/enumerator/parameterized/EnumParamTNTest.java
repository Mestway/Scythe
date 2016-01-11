package enumerator.parameterized;

import enumerator.Constraint;
import enumerator.EnumContext;
import enumerator.TableEnumerator;
import org.junit.Test;
import sql.lang.DataType.NumberVal;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ConstantVal;
import util.DebugHelper;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by clwang on 1/10/16.
 */
public class EnumParamTNTest {
    String inputSrc =
            "| id   |  rev   |  content  |" + "\r\n" +
            "|---------------------------|" + "\r\n" +
            "| 1    |  1     |  A        |" + "\r\n" +
            "| 2    |  1     |  B        |" + "\r\n" +
            "| 1    |  2     |  C        |" + "\r\n" +
            "| 1    |  3     |  D        |";

    String outputSrc =
            "| col1 | col2 | col3 |" + "\r\n" +
            "|--------------------|" + "\r\n" +
            "|  1   |  3   |  D   |" + "\r\n" +
            "|  2   |  1   |  B   |";

    Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);
    Constraint c = new Constraint(1, new ArrayList<>(), Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin));

    @Test
    public void test() {
        List<TableNode> tns = EnumParamTN.enumParameterizedTableNodes(Arrays.asList(new NamedTable(input)), new ArrayList<>(), 2);

        EnumContext ec = new EnumContext();
        ec.setParameterizedTables(tns);

        InstantiateEnv ie = new InstantiateEnv(
                Arrays.asList(
                        new ConstantVal(new NumberVal(1)),
                        new ConstantVal(new NumberVal(2))),
                ec);

        tns = tns.stream().map(tn -> tn.instantiate(ie)).filter(t -> t.getAllHoles().size() == 0).collect(Collectors.toList());
        DebugHelper.printTableNodes(tns);
    }

}