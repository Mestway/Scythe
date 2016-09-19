package forward_enumeration.table_enumerator;

import forward_enumeration.Constraint;
import forward_enumeration.context.EnumContext;
import forward_enumeration.context.QueryChest;
import forward_enumeration.context.TableTreeNode;
import forward_enumeration.parameterized.EnumParamTN;
import sql.lang.Table;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;
import util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/21/16.
 */
public abstract class AbstractTableEnumerator {

    public List<TableNode> enumProgramWithIO(List<Table> input, Table output, Constraint c) {

        System.out.println("[Initialize Enumeration]");

        List<ValNode> vns = new ArrayList<>();
        vns.addAll(c.constValNodes());

        List<TableNode> parameterizedTables = EnumParamTN
                .enumParameterizedTableNodes(
                        input.stream()
                                .map(t -> new NamedTable(t)).collect(Collectors.toList()),
                        vns,
                        c.getNumberOfParam());

        System.out.println("  Parameterized Table Number: " + parameterizedTables.size());

        EnumContext ec = new EnumContext(input, c);
        ec.setParameterizedTables(parameterizedTables);
        ec.setOutputTable(output);
        ec.setMaxFilterLength(c.maxFilterLength());

        System.out.println("[Enumeration Start]");

        QueryChest qc = this.enumTable(ec, c.maxDepth());

        List<TableNode> valid = qc.lookup(output);

        if (valid == null) {
            System.out.println("[Enumeration Finished] Does not find a query in the search space.");
            return valid;
        } else {
            List<TableNode> result = new ArrayList<>();

            if (this instanceof CanonicalTableEnumerator)
                System.out.println("[Consistent Table number] " + qc.getEdges().getDirectLinkCount(ec.getOutputTable()));
            else if (this instanceof SymbolicTableEnumerator)
                System.out.println("[Consistent Table number] " + qc.getAllCandidates().size());

            // this is not always enabled, as this export algorithm is only designed for canonical SQL enumerator
            if (qc.isEnableExport()) {
                Set<Table> leafNodes = new HashSet<>();
                leafNodes.addAll(ec.getInputs());
                List<TableTreeNode> trees = qc.getEdges().findTableTrees(ec.getOutputTable(), leafNodes, 4);

                int totalQueryCount = 0;
                for (TableTreeNode t : trees) {
                    t.inferQuery(ec);
                    //List<TableNode> tns = t.treeToQuery();

                    int count = t.countQueryNum();
                    //System.out.println("Queries corresponds to this tree: " + count);
                    totalQueryCount += count;
                }

                System.out.println("Total Tree Count: " + trees.size());
                System.out.println("Total Query Count: " + totalQueryCount);
            }

            System.out.println("[Enumeration Finished]");

            return result;
        }
    }

    // The default simple reEnumProgramWithIO will simply ignore all old IO pairs
    public List<TableNode> reEnumProgramWithIO(
            List<Pair<List<Table>, Table>> oldIOPairs,
            Pair<List<Table>, Table> newIOPair, Constraint c) {
        return enumProgramWithIO(newIOPair.getKey(), newIOPair.getValue(), c);
    }

    // the enumeration result will be stored in EC
    abstract public QueryChest enumTable(EnumContext ec, int depth);

}