package backward_inference;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.context.EnumContext;
import forward_enumeration.table_enumerator.hueristics.TableNaturalJoinWithAggr;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.*;
import sql.lang.datatype.Value;
import util.CombinationGenerator;
import util.TableInstanceParser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 2/17/16.
 */
public class MappingInferenceTest {

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
                "| col1 | col2 | col3 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  2   |  1   |  B   |" + "\r\n" +
                        "|  1   |  3   |  D   |" + "\r\n" +
                        "|  1   |  2   | NULL[str] |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);
        Table output2 = TableInstanceParser.parseMarkDownTable("table22", outputSrc2);

        System.out.println(output2.contentEquals(output));

        MappingInference mi = MappingInference.buildMapping(input, output);
        System.out.println(mi);

    }

    public void testTableInverse() throws Exception {
        String inputSrc =
                "| T1.User   |  T1.Phone   |  T1.Value  | T2.User | T2.MaxVal |" + "\r\n" +
                        "|---------------------------------------------|" + "\r\n" +
                        "| Peter    |  0       |  1        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  0       |  1        | Paul  | 7 |" + "\r\n" +
                        "| Peter    |  456     |  2        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  456     |  2        | Paul  | 7 |" + "\r\n" +
                        "| Peter    |  456     |  3        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  456     |  3        | Paul  | 7 |" + "\r\n" +
                        "| Paul     |  456     |  7        | Peter | 3 |" + "\r\n" +
                        "| Paul     |  456     |  7        | Paul  | 7 |" + "\r\n" +
                        "| Paul     |  789     |  10        | Peter | 3 |" + "\r\n" +
                        "| Paul     |  789     |  10        | Paul  | 7 |";

        String outputSrc =
                "| col1 | col2 | col3 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  Peter   |  456  |  3   |" + "\r\n" +
                        "|  Paul    |  456  |  7   |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        //assert MappingInference.fastBuildMapping(input, output).equals(MappingInference.buildMapping(input, output));

        inputSrc =
                "| Id |  Name |  Other_Columns | \r\n" +
                        "|-----------------------------| \r\n" +
                        "| 1  |  A    |   A_data_1     | \r\n" +
                        "| 2  |  A    |   A_data_2     | \r\n" +
                        "| 3  |  A    |   A_data_3     | \r\n" +
                        "| 4  |  B    |   B_data_1     | \r\n" +
                        "| 5  |  B    |   B_data_2     | \r\n" +
                        "| 6  |  C    |   C_data_1     |";

        outputSrc =
                "| col1 | col2 | col3     | \r\n" +
                        "|------------------------| \r\n" +
                        "| 3    | A    | A_data_3 | \r\n" +
                        "| 5    | B    | B_data_2 | \r\n" +
                        "| 6    | C    | C_data_1 |";

        input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        //assert MappingInference.fastBuildMapping(input, output).equals(MappingInference.buildMapping(input, output));

    }

    @Test
    public void testBuildMapping0() throws Exception {
        String inputSrc =
                "| T1.User   |  T1.Phone   |  T1.Value  | T2.User | T2.MaxVal |" + "\r\n" +
                        "|---------------------------------------------|" + "\r\n" +
                        "| Peter    |  0       |  1        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  0       |  1        | Paul  | 7 |" + "\r\n" +
                        "| Peter    |  456     |  2        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  456     |  2        | Paul  | 7 |" + "\r\n" +
                        "| Peter    |  456     |  3        | Peter | 3 |" + "\r\n" +
                        "| Peter    |  456     |  3        | Paul  | 7 |" + "\r\n" +
                        "| Paul     |  456     |  7        | Peter | 3 |" + "\r\n" +
                        "| Paul     |  456     |  7        | Paul  | 7 |" + "\r\n" +
                        "| Paul     |  789     |  10        | Peter | 3 |" + "\r\n" +
                        "| Paul     |  789     |  10        | Paul  | 7 |";

        String outputSrc =
                "| col1 | col2 | col3 |" + "\r\n" +
                        "|--------------------|" + "\r\n" +
                        "|  Peter   |  456  |  3   |" + "\r\n" +
                        "|  Paul    |  456  |  7   |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        MappingInference mi = MappingInference.buildMapping(input, output);
        System.out.println("-----");
        System.out.println(mi.toString());
        List<CellToCellMap> instances = mi.genMappingInstancesWColumnBarrier(Arrays.asList(3, 3 + 2));
        for (CellToCellMap cim : instances) {
           // System.out.println("---");
           // System.out.println(cim.toString());
        }

        System.out.println(instances.size());

        MappingInference.printColumnMapping(mi.genColumnMappingInstances());
        MappingInference.printColumnMapping(mi.genRowMappingInstances());


        List<Set<Integer>> columnMapping = mi.genColumnMappingInstances();
        List<List<Integer>> listRepColMapping = new ArrayList<>();
        for (int i = 0; i < columnMapping.size(); i ++) {
            listRepColMapping.add(columnMapping.get(i).stream().collect(Collectors.toList()));
        }
        List<List<Integer>> targetsToSearch = CombinationGenerator.rotateList(listRepColMapping);

        for (List<Integer> l : targetsToSearch) {
            System.out.println("##" + l.stream().map(i -> i.toString()).reduce("", (x,y)-> x + ", " + y));
            MappingInference.printColumnMapping(mi.genRowMappingRange(l));
        }

        List<CellToCellMap> is = mi.genMappingInstances();
        System.out.println(is.size());
        for (CellToCellMap cim : is) {
            //System.out.println("---");
            //System.out.println(cim.toString());
        }

        System.out.println(is.size());

    }

    public void testBuildMapping() throws Exception {
        String inputSrc =
            "| id   |  rev   |  content  | n | m |" + "\r\n" +
            "|-----------------------------------|" + "\r\n" +
            "| 1    |  1     |  A        | 1 | a |" + "\r\n" +
            "| 2    |  1     |  B        | 1 | a |" + "\r\n" +
            "| 1    |  2     |  C        | 1 | a |" + "\r\n" +
            "| 1    |  3     |  D        | 1 | a |" + "\r\n" +
            "| 1    |  1     |  A        | 2 | b |" + "\r\n" +
            "| 2    |  1     |  B        | 2 | b |" + "\r\n" +
            "| 1    |  2     |  C        | 2 | b |" + "\r\n" +
            "| 1    |  3     |  D        | 2 | b |" + "\r\n" +
            "| 1    |  1     |  A        | 1 | c |" + "\r\n" +
            "| 2    |  1     |  B        | 1 | c |" + "\r\n" +
            "| 1    |  2     |  C        | 1 | c |" + "\r\n" +
            "| 1    |  3     |  D        | 1 | c |";

        String outputSrc =
            "| col1 | col2 | col3 |" + "\r\n" +
            "|--------------------|" + "\r\n" +
            "|  1   |  3   |  D   |" + "\r\n" +
            "|  2   |  1   |  B   |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        MappingInference mi = MappingInference.buildMapping(input, output);
        System.out.println("-----");
        System.out.println(mi.toString());
        List<CellToCellMap> instances = mi.genMappingInstancesWColumnBarrier(Arrays.asList(3, 3 + 2));
        for (CellToCellMap cim : instances) {
            System.out.println("---");
            System.out.println(cim.toString());
        }

        MappingInference.printColumnMapping(mi.genColumnMappingInstances());
        MappingInference.printColumnMapping(mi.genRowMappingInstances());


    }

    public void testMapping() throws Exception {
        String inputSrc =
                "| id   |  rev   |  content  |" + "\r\n" +
                "|----------------------------------|" + "\r\n" +
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

        EnumConfig c = new EnumConfig(
                2,
                new ArrayList<>(),
                Arrays.asList(
                        AggregationNode.AggrMax, AggregationNode.AggrCount),
                2,
                Arrays.asList(input));
        EnumContext ec = new EnumContext(Arrays.asList(input), c);

        // Using only equi-join
        /*
        TableNode natExt = TableNaturalJoinWithAggr.naturalTableExtensionWithAggr(ec);
        System.out.println(natExt.eval(new Environment()));
        System.out.println();
        */

        // traditional approach:
        /*
        List<TableNode> tns = EnumAggrTableNode.enumAggrNodeWFilter(ec);
        tns.add(new NamedTable(input));
        TableNode jn = new JoinNode(tns);
        Table ot = jn.eval(new Environment());
        System.out.println(ot);
        */

        Table ot = TableNaturalJoinWithAggr.naturalTableExtensionWithAggr(ec).eval(new Environment());

        System.out.println(ot.toString());

        MappingInference mi = MappingInference.buildMapping(ot, output);
        System.out.println("[Refine Mapping Done]");
        System.out.println(mi.toString());

        List<CellToCellMap> instances = mi.genMappingInstances();
        System.out.println("[Gen MappingInstance Done] size: " + instances.size());
        for (CellToCellMap cim : instances) {
            System.out.println("---");
            System.out.println(cim.toString());
            Map<Integer, Integer> lineMap = MappingInference.columnMappingInference(cim);
            for (Map.Entry<Integer, Integer> entry : lineMap.entrySet()) {
                //System.out.println(entry.getKey() + " - " + entry.getValue());
            }
        }
    }
}