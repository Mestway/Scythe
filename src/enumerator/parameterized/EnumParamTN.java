package enumerator.parameterized;

import enumerator.*;
import sql.lang.DataType.ValType;
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

    public static final int maxParamFilterLength = 1;

    public static List<TableNode> enumParameterizedTableNodes(
            List<TableNode> basicTables,
            List<ValNode> constants,
            int numberOfParams) {

        constants.addAll(ValHole.genHoles(numberOfParams));

        EnumContext ec = new EnumContext();
        ec.setTableNodes(basicTables);
        ec.setValNodes(constants);
        ec.setMaxFilterLength(maxParamFilterLength);

        List<TableNode> tns = new ArrayList<>();
        tns.addAll(EnumSelTableNode.enumSelectNode(ec, 1));

        List<TableNode> paramterizedTables = tns.stream()
                .filter(tn -> !tn.getAllHoles().isEmpty())
                .map(tn -> RenameTNWrapper.tryRename(tn))
                .collect(Collectors.toList());

        return paramterizedTables;
    }
}
