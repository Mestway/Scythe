package forward_enumeration.enum_components.parameterized;

import forward_enumeration.context.EnumContext;
import lang.sql.dataval.ValType;
import lang.sql.ast.contable.TableNode;
import lang.sql.ast.valnode.ValHole;
import lang.sql.ast.valnode.ValNode;
import util.RenameWrapper;

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
        ec.setTableNodes(basicTables.stream().map(bt -> RenameWrapper.forceRename(bt)).collect(Collectors.toList()));
        ec.setValNodes(constants);
        ec.setMaxFilterLength(maxParamFilterLength);

        // get all parameterized table nodes by enumerating select nodes with ec
        //      and eval away nodes that contains no holes
        List<TableNode> paramterizedTables =
                EnumSelectTableNode.enumSelectNode(ec, true).stream()
                    .filter(tn -> !tn.getAllHoles().isEmpty())
                    .map(tn -> RenameWrapper.tryRename(tn))
                    .collect(Collectors.toList());

        return paramterizedTables;
    }
}
