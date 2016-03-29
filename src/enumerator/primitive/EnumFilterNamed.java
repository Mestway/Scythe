package enumerator.primitive;

import enumerator.context.EnumContext;
import sql.lang.DataType.ValType;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enumerate filtered named tables from enum context
 * Created by clwang on 2/1/16.
 */
public class EnumFilterNamed {

    /**
     * Enumerate filter nodes for named tables: given a named table, we will generate filter for the named tables.
     * @return
     */
    public static List<TableNode> enumFilterNamed(EnumContext ec) {
        List<TableNode> targets = ec.getTableNodes().stream()
                .filter(tn -> (tn instanceof NamedTable))
                .collect(Collectors.toList());

        List<TableNode> result = new ArrayList<>();

        for (TableNode tn : targets) {

            // the selection args are complete
            List<ValNode> vals = tn.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());

            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendTypeMap(ec, typeMap);

            List<Filter> filters = FilterEnumerator.enumFiltersLR(vals, ec2.getValNodes(), ec2);

            for (Filter f : filters) {
                TableNode sn = new SelectNode(vals, tn, f);
                result.add(sn);
            }
        }
        return result;
    }

}
