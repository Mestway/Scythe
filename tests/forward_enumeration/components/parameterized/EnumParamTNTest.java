package forward_enumeration.components.parameterized;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.context.EnumContext;
import forward_enumeration.enum_components.parameterized.EnumParamTN;
import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import org.junit.Test;
import lang.sql.dataval.NumberVal;
import lang.table.Table;
import lang.sql.ast.contable.AggregationNode;
import lang.sql.ast.contable.NamedTableNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.ast.valnode.ConstantVal;
import util.DebugHelper;
import util.TableExampleParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    Table input = TableExampleParser.parseMarkDownTable("table1", inputSrc);
    Table output = TableExampleParser.parseMarkDownTable("table2", outputSrc);
    EnumConfig c = new EnumConfig(1, new ArrayList<>(), Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrMin),2,Arrays.asList(input));

    @Test
    public void test() {
        List<TableNode> tns = EnumParamTN.enumParameterizedTableNodes(Arrays.asList(new NamedTableNode(input)), new ArrayList<>(), 2);

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