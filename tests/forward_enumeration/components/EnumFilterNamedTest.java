package forward_enumeration.components;

import org.junit.Test;
import scythe_interface.IOExample;
import forward_enumeration.context.EnumContext;
import forward_enumeration.QueryContainer;
import forward_enumeration.baselines.components.EnumJoinTableNodes;
import lang.table.Table;
import lang.sql.ast.contable.NamedTableNode;
import lang.sql.ast.contable.TableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 4/6/16.
 */
public class EnumFilterNamedTest {

    @Test
    public void test() {
        IOExample exampleDS = IOExample.readFromFile("data//StackOverflow//001R");

        QueryContainer qc = QueryContainer.initWithInputTables(exampleDS.inputs, QueryContainer.ContainerType.None);
        EnumContext ec = new EnumContext(exampleDS.inputs, exampleDS.enumConfig);
        ec.setParameterizedTables(new ArrayList<>());
        ec.setOutputTable(exampleDS.output);

        List<TableNode> tns = new ArrayList<>();
        tns.add(new NamedTableNode(exampleDS.inputs.get(0)));

        ec.setTableNodes(tns);
        EnumJoinTableNodes.generalEmitEnumJoin(tns, tns, ec, qc);
        System.out.println(qc.getRepresentativeTableNodes().size());
        for (Table t : qc.getMemoizedTables()) {
            System.out.println(t);
        }
    }
}