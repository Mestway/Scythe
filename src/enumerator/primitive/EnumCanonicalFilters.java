package enumerator.primitive;

import enumerator.context.EnumContext;
import sql.lang.DataType.ValType;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.JoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.RenameTableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/29/16.
 */
public class EnumCanonicalFilters {

    public static List<Filter> enumCanonicalFilterNamedTable(NamedTable tn, EnumContext ec) {
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

        return FilterEnumerator.enumFiltersLR(vals, ec2.getValNodes(), ec2);
    }

    public static List<Filter> enumCanonicalFilterJoinNode(RenameTableNode rt, EnumContext ec) {

        if (! (rt.getTableNode() instanceof JoinNode))
            System.out.println("[ERROR EnumCanonicalFilters 44] " + rt.getTableNode().getClass());

        JoinNode jn = (JoinNode) rt.getTableNode();
        List<Integer> tableBoundaryIndex = new ArrayList<>();

        tableBoundaryIndex.add(0); // the boundary of the first table is 0
        for (int i = 0; i < jn.getTableNodes().size(); i ++) {
            // the boundary of the next table = the boundary of the this table + this table size
            tableBoundaryIndex.add(tableBoundaryIndex.get(i) + jn.getTableNodes().get(i).getSchema().size());
        }

        List<Filter> filters = new ArrayList<>();
        for (int i = 0; i < tableBoundaryIndex.size(); i ++) {
            List<ValNode> L = new ArrayList<>();
            for (int k = tableBoundaryIndex.get(i); k < tableBoundaryIndex.get(i + 1); k ++) {
                L.add(new NamedVal(rt.getSchema().get(k)));
            }
            for (int j = i + 1; j < tableBoundaryIndex.size(); j ++) {
                List<ValNode> R = new ArrayList<>();
                for (int k = tableBoundaryIndex.get(j); k < tableBoundaryIndex.get(j+1); k ++) {
                    R.add(new NamedVal(rt.getSchema().get(k)));
                }
                filters.addAll(FilterEnumerator.enumFiltersLR(L, R, ec));
            }
        }

        return filters;
    }

    public static List<Filter> enumCanonicalFilterAggrNode(RenameTableNode rt, EnumContext ec) {

        if (! (rt.getTableNode() instanceof AggregationNode))
            System.out.println("[ERROR EnumCanonicalFilters 44] " + rt.getTableNode().getClass());
        List<Filter> filters = new ArrayList<>();

        //TODO: implement how enumCanonicalFilterAggrNode work.

        return filters;
    }
}
