package forward_enumeration.table_enumerator;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.EnumParamTN;
import global.GlobalConfig;
import sql.lang.Table;
import sql.lang.ast.table.NamedTableNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public abstract class AbstractTableEnumerator {

    public List<TableNode> enumProgramWithIO(List<Table> input, Table output, EnumConfig c) {

        if (GlobalConfig.PRINT_LOG)
            System.out.println("[Initialize Enumeration]");

        List<ValNode> vns = new ArrayList<>();
        vns.addAll(c.constValNodes());

        // parameterized tables are obtained from
        List<TableNode> parameterizedTables = EnumParamTN
                .enumParameterizedTableNodes(
                        c.getExistsCores().stream().map(t -> new NamedTableNode(t)).collect(Collectors.toList()),
                        vns,
                        c.getNumberOfParam());

        if (GlobalConfig.PRINT_LOG)
            System.out.println("  Parameterized Table Number: " + parameterizedTables.size());

        EnumContext ec = new EnumContext(input, c);
        ec.setParameterizedTables(parameterizedTables);
        ec.setOutputTable(output);
        if (c.containsDerivedConstants)
            ec.setConstProvidedByUser();

        if (GlobalConfig.PRINT_LOG)
            System.out.println("[Enumeration Start]");

        List<TableNode> result = this.enumTable(ec, c.maxDepth());

        if (result.isEmpty()) {
            if (GlobalConfig.PRINT_LOG)
                System.out.println("[Enumeration Finished] Does not find a query in the search space.");
        }

        if (GlobalConfig.PRINT_LOG)
            System.out.println("[Enumeration Finished]");

        return result;
    }

    // the enumeration result will be stored in QC
    abstract public List<TableNode> enumTable(EnumContext ec, int depth);

}