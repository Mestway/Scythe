package forward_enumeration.enum_components.parameterized;

import forward_enumeration.context.EnumContext;
import forward_enumeration.enum_components.FilterEnumerator;
import lang.sql.ast.predicate.Predicate;
import lang.sql.dataval.ValType;
import lang.sql.ast.contable.SelectNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.ValHole;
import lang.sql.ast.valnode.ValNode;
import util.CombinationGenerator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enum a classical Select...From...Where query with given ec
 * Created by clwang on 1/7/16.
 */
public class EnumSelectTableNode {

    /********************************************************************************
     Enum table by select
     1. Enumerate the table to select from
     2. Enumerate fields to be projected
     3. Enumerate filters to perform filtering
     *********************************************************************************/

    public static List<TableNode> enumSelectNode(EnumContext ec) {
        return enumSelectNode(ec, false);
    }

    public static List<TableNode> enumSelectNode(EnumContext ec, boolean selectStar) {

        List<TableNode> result = new ArrayList<>();

        List<TableNode> coreTableNode = ec.getTableNodes(); //TableEnumerator.enumTable(ec, depth - 1);

        for (TableNode tn : coreTableNode) {
            List<List<ValNode>> lvn = enumSelectArgs(ec,tn, selectStar);
            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

            List<Predicate> filters = FilterEnumerator.enumFiltersLR(ec2.getValNodes(), ec2.getValNodes(), ec2, true);

            for (List<ValNode> vn : lvn) {
                for (Predicate f : filters) {
                    TableNode sn = new SelectNode(vn, tn, f);
                    result.add(sn);
                }
            }
        }
        return result;
    }

    // Enumerate the selection fields of a select query
    private static List<List<ValNode>> enumSelectArgs(EnumContext ec, TableNode tableNode, boolean enumStar) {
        List<ValNode> vals = new ArrayList<ValNode>();
        // TODO: check whether ruling out hole param is a good idea
        vals.addAll(ec.getValNodes().stream()
                .filter(vn -> !(vn instanceof ValHole)).collect(Collectors.toList()));

        // collect table column names from the schema
        vals.addAll(tableNode.getSchema().stream()
                .map(s -> new NamedVal(s)).collect(Collectors.toList()));

        List<List<ValNode>> valNodes = new ArrayList<>();
        if (! enumStar)
            valNodes = CombinationGenerator.genCombination(vals);
        else
            valNodes.add(vals);

        return valNodes;
    }

}
