package forward_enumeration.table_enumerator;

import forward_enumeration.context.EnumConfig;
import forward_enumeration.context.EnumContext;
import forward_enumeration.context.QueryChest;
import forward_enumeration.primitive.parameterized.EnumParamTN;
import sql.lang.Table;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public abstract class AbstractTableEnumerator {

    public List<TableNode> enumProgramWithIO(List<Table> input, Table output, EnumConfig c) {

        System.out.println("[Initialize Enumeration]");

        List<ValNode> vns = new ArrayList<>();
        vns.addAll(c.constValNodes());

        List<TableNode> parameterizedTables = EnumParamTN
                .enumParameterizedTableNodes(
                        input.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()),
                        vns,
                        c.getNumberOfParam());

        System.out.println("  Parameterized Table Number: " + parameterizedTables.size());

        EnumContext ec = new EnumContext(input, c);
        ec.setParameterizedTables(parameterizedTables);
        ec.setOutputTable(output);
        ec.setMaxFilterLength(c.maxFilterLength());

        System.out.println("[Enumeration Start]");

        QueryChest qc = this.enumTable(ec, c.maxDepth());

        // check whether the table has been visited during the synthesis process
        if (! qc.tableVisited(output)) {
            System.out.println("[Enumeration Finished] Does not find a query in the search space.");
            return null;
        } else {
            List<TableNode> result = new ArrayList<>();

            if (this instanceof CanonicalTableEnumerator)
                System.out.println("[Consistent Table number] " + qc.getTableLinks().getDirectLinkCount(ec.getOutputTable()));
            else if (this instanceof StagedEnumerator)
                System.out.println("[Consistent Table number] " + qc.getAllCandidates().size());

            System.out.println("[Enumeration Finished]");

            return result;
        }
    }

    // The default simple reEnumProgramWithIO will simply ignore all old IO pairs
    public List<TableNode> reEnumProgramWithIO(
            List<Pair<List<Table>, Table>> oldIOPairs,
            Pair<List<Table>, Table> newIOPair, EnumConfig c) {
        return enumProgramWithIO(newIOPair.getKey(), newIOPair.getValue(), c);
    }

    // the enumeration result will be stored in QC
    abstract public QueryChest enumTable(EnumContext ec, int depth);

}