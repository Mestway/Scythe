package enumerator.tableenumerator;

import enumerator.Constraint;
import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import enumerator.parameterized.EnumParamTN;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;

import javax.management.Query;
import java.util.ArrayList;
import java.util.List;
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

        System.out.println("  Parametereized Table Number: " + parameterizedTables.size());

        EnumContext ec = new EnumContext(input, c);
        ec.setParameterizedTables(parameterizedTables);
        ec.setOutputTable(output);
        ec.setMaxFilterLength(c.maxFilterLength());

        System.out.println("[Enumeration Start]");

        QueryChest qc = this.enumTable(ec, c.maxDepth());
        for (Table t : qc.getMemoizedTables().keySet()) {
            if (t.equals(ec.getOutputTable())) {
                System.out.println(t.hashCode() + "~" + ec.getOutputTable().hashCode());
            //if (t.getContent().size() == 2 && t.getMetadata().size() == 3) {
                System.out.println(t);
            }
        }

        List<TableNode> valid = qc.lookup(output);

        if (valid == null) {
            System.out.println("[Enumeration Finished] Table number null.");
            return valid;
        } else {
            List<TableNode> tss = qc.lookup(output);
            List<TableNode> result = new ArrayList<>();
            for (TableNode ts : tss) {
                result.addAll(qc.export(ts, new ArrayList<>(),
                        input.stream().map(i -> new NamedTable(i)).collect(Collectors.toList())));
            }
            System.out.println("[Enumeration Finished] Table number: " + result.size());
            return result;
        }
    }

    // the enumeration result will be stored in EC
    abstract public QueryChest enumTable(EnumContext ec, int depth);

}