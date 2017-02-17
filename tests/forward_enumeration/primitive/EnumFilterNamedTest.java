package forward_enumeration.primitive;

import org.junit.Test;
import scythe_interface.ExampleDS;
import forward_enumeration.context.EnumContext;
import forward_enumeration.container.QueryContainer;
import forward_enumeration.canonical_enum.components.EnumJoinTableNodes;
import sql.lang.Table;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 4/6/16.
 */
public class EnumFilterNamedTest {

    @Test
    public void test() {
        ExampleDS exampleDS = ExampleDS.readFromFile("data//StackOverflow//001R");

        QueryContainer qc = QueryContainer.initWithInputTables(exampleDS.inputs, QueryContainer.ContainerType.None);
        EnumContext ec = new EnumContext(exampleDS.inputs, exampleDS.enumConfig);
        ec.setParameterizedTables(new ArrayList<>());
        ec.setOutputTable(exampleDS.output);

        List<TableNode> tns = new ArrayList<>();
        tns.add(new NamedTable(exampleDS.inputs.get(0)));

        ec.setTableNodes(tns);
        EnumJoinTableNodes.generalEmitEnumJoin(tns, tns, ec, qc);
        System.out.println(qc.getRepresentativeTableNodes().size());
        for (Table t : qc.getMemoizedTables()) {
            System.out.println(t);
        }
    }
}