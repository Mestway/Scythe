package enumerator.parameterized;

import enumerator.context.EnumContext;
import enumerator.primitive.tables.EnumSelTableNode;
import sql.lang.DataType.ValType;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValHole;
import sql.lang.ast.val.ValNode;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/8/16.
 */
public class EnumParamTN {

    public static final int maxParamFilterLength = 2;
    public static int numberOfParams = 2;

    public static List<TableNode> enumParameterizedTableNodes(
            List<TableNode> basicTables,
            List<ValNode> constants,
            int paramNum) {

        numberOfParams = paramNum;

        List<ValType> valueTypes = new ArrayList<>();
        for (TableNode tn : basicTables) {
            for (ValType vt : tn.getSchemaType()) {
                if (!valueTypes.contains(vt))
                    valueTypes.add(vt);
            }
        }
        constants.addAll(ValHole.genHolesWithType(numberOfParams, valueTypes));

        EnumContext ec = new EnumContext();
        ec.setTableNodes(basicTables.stream().map(bt -> RenameTNWrapper.forceRename(bt)).collect(Collectors.toList()));
        ec.setValNodes(constants);
        ec.setMaxFilterLength(maxParamFilterLength);

        List<TableNode> tns = new ArrayList<>();
        tns.addAll(EnumSelTableNode.enumSelectNode(ec, true));

        List<TableNode> paramterizedTables = tns.stream()
                .filter(tn -> !tn.getAllHoles().isEmpty())
                .map(tn -> RenameTNWrapper.tryRename(tn))
                .collect(Collectors.toList());

        return paramterizedTables;
    }
}
