package enumerator.tableenumerator;

import enumerator.Constraint;
import enumerator.EnumContext;
import enumerator.parameterized.EnumParamTN;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;

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

        List<TableNode> tns = this.enumTable(ec, c.maxDepth());

        // only keep the table nodes that are consistent with the I/O pairs
        List<TableNode> valid = tns.stream().filter(tn -> {
            try {
                return tn.eval(new Environment()).contentStrictEquals(output);
            } catch (SQLEvalException e) {
                return false;
            }
        }).collect(Collectors.toList());

        if (valid.isEmpty()) {
            System.out.println("[Enumeration Finished] Table number: 0");
            return valid;
        } else {
            List<TableNode> tss = ec.lookup(output);
            List<TableNode> result = new ArrayList<>();
            for (TableNode ts : tss) {
                result.addAll(ec.export(ts, new ArrayList<>(),
                        input.stream().map(i -> new NamedTable(i)).collect(Collectors.toList())));
            }
            System.out.println("[Enumeration Finished] Table number: " + result.size());
            return result;
        }
    }

    // the enumeration result will be stored in EC
    abstract public List<TableNode> enumTable(EnumContext ec, int depth);

}