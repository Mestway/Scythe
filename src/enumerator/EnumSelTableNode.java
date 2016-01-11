package enumerator;

import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.filter.*;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValHole;
import sql.lang.ast.val.ValNode;
import util.CombinationGenerator;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/7/16.
 */
public class EnumSelTableNode {

    /********************************************************************************
     Enum table by select
     1. Enumerate the table to select from
     2. Enumerate fields to be projected
     3. Enumerate filters to perform filtering
     *********************************************************************************/

    public static List<TableNode> enumSelectNode(EnumContext ec, int depth) {

        List<TableNode> result = new ArrayList<>();

        List<TableNode> coreTableNode = TableEnumerator.enumTable(ec, depth - 1);

        for (TableNode tn : coreTableNode) {
            List<List<ValNode>> lvn = enumSelectArgs(ec,tn);
            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendTypeMap(ec, typeMap);

            List<Filter> filters = FilterEnumerator.enumFilter(ec2, depth - 1);


            for (List<ValNode> vn : lvn) {
                for (Filter f : filters) {
                    TableNode sn = new SelectNode(vn, tn, f);
                    result.add(sn);
                }
            }
        }
        return result;
    }

    // Enumerate the selection fields of a select query
    private static List<List<ValNode>> enumSelectArgs(EnumContext ec, TableNode tableNode) {
        List<ValNode> vals = new ArrayList<ValNode>();
        // TODO: check whether ruling out hole param is a good idea
        vals.addAll(ec.getValNodes().stream()
                .filter(vn -> !(vn instanceof ValHole)).collect(Collectors.toList()));

        // collect table column names from the schema
        vals.addAll(tableNode.getSchema().stream()
                .map(s -> new NamedVal(s)).collect(Collectors.toList()));

        List<List<ValNode>> valNodes = CombinationGenerator.genCombination(vals);
        return valNodes;
    }

}
