package forward_enumeration.primitive;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.EnumParamTN;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.JoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.RenameTableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
import sql.lang.ast.filter.*;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.ValHole;
import sql.lang.ast.val.ValNode;
import util.CombinationGenerator;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * The primitive enumerator for enumerating Filters from ec
 * Created by clwang on 1/7/16.
 */
public class FilterEnumerator {

    /**
     * Enumerate filter by specifying which values can appear on the LHS and which values can appear on RHS
     * @param L The set of LHS values
     * @param R The set of RHS values
     * @param ec The enumeration context
     * @return filters enumerated within this context
     */
    public static List<Filter> enumFiltersLR(List<ValNode> L, List<ValNode> R, EnumContext ec, boolean allowExists) {
        List<Filter> atomics = enumAtomicFiltersLR(L, R, ec, allowExists);
        List<Filter> result = enumCompoundFilters(atomics, 1, ec.getMaxFilterLength());
        return result;
    }

    private static List<Filter> enumCompoundFilters(List<Filter> filters, int filterLength, int maxFilterLength) {

        // do not allow holes with same id but different type exists
        filters = filters.stream().filter(f -> {
            List<ValHole> holes = f.getAllHoles().stream()
                    .filter(h -> (h instanceof ValHole)).map(h -> (ValHole) h).collect(Collectors.toList());
            return !ValHole.containsSameHoleDiffType(holes);
        }).collect(Collectors.toList());

        if (filterLength == maxFilterLength)
            return filters;

        List<Filter> result = new ArrayList<Filter>();
        result.addAll(filters);

        for (int i = 0; i < filters.size(); i ++) {
            for (int j = i + 1; j < filters.size(); j ++) {

                if (filters.get(i) instanceof ExistsFilter && filters.get(j) instanceof ExistsFilter)
                    continue;

                // Prune: if two filters have same arguments but different comparator,
                // then they are exclusive and will not be added as LogicAndFilter
                if (filters.get(i).containsExclusiveFilter(filters.get(j)))
                    continue;

                Filter f = new LogicAndFilter(filters.get(i), filters.get(j));
                if (f.getFilterLength() == filterLength + 1)
                    result.add(f);
               /* if = new LogicOrFilter(filters.get(i), filters.get(j));
                if (f.getFilterLength() == filterLength + 1)
                    result.add(f); */
            }
        }

        return enumCompoundFilters(result, filterLength + 1, maxFilterLength);
    }

    // TODO: optimize by ruling out same filters
    private static List<Filter> enumAtomicFiltersLR(List<ValNode> L, List<ValNode> R, EnumContext ec, boolean allowExists) {
        // L contains all left values, and R contains all right values in a comparator
        List<Filter> atomics = new ArrayList<>();
        for (ValNode l : L) {
            for (ValNode r : R) {
                if (l.equalsToValNode(r)) continue;
                if (l instanceof ConstantVal && r instanceof ConstantVal) continue;

                if (l.getType(ec).equals(r.getType(ec))) {
                    if (l.getType(ec).equals(ValType.DateVal) || l.getType(ec).equals(ValType.NumberVal)) {
                        for (BiFunction<Value, Value, Boolean> func : VVComparator.getAllFunctions()) {
                            atomics.add(new VVComparator(Arrays.asList(l, r), func));
                        }
                    } else {
                        atomics.add(new VVComparator(Arrays.asList(l, r), VVComparator.eq));
                        atomics.add(new VVComparator(Arrays.asList(l, r), VVComparator.neq));

                    }
                }
            }
        }

        // Enumerate Filter with EXISTS clause, we don't want exists to
        if (allowExists) {
            List<ValNode> allValues = new ArrayList<>();
            allValues.addAll(L);
            allValues.addAll(R);
            allValues.addAll(ec.getValNodes());

            List<List<ValNode>> llvns = CombinationGenerator.genCombination(
                    allValues, EnumParamTN.numberOfParams);
            for (List<ValNode> vns : llvns) {
                InstantiateEnv ie = new InstantiateEnv(vns, ec);
                atomics.addAll(
                        ec.getParameterizedTables().stream().map(tn -> tn.instantiate(ie))
                                .filter(t -> t.getAllHoles().size() == 0)
                                .map(tn -> new ExistsFilter(tn))
                                .collect(Collectors.toList()));
                // Also enumerate not exists
                atomics.addAll(
                        ec.getParameterizedTables().stream().map(tn -> tn.instantiate(ie))
                                .filter(t -> t.getAllHoles().size() == 0)
                                .map(tn -> new ExistsFilter(tn, true))
                                .collect(Collectors.toList()));
            }
        }

        // Add IS NULL for atomic filters
        // TODO: possibly just ignore it
        Set<ValNode> allValues = new HashSet<>();
        allValues.addAll(L);
        allValues.addAll(R);
        allValues.addAll(ec.getValNodes());

        for (ValNode vn : allValues) {
            if (vn instanceof ValHole)
                continue;
            atomics.add(new IsNullFilter(vn, true));
            atomics.add(new IsNullFilter(vn, false));
        }

        return atomics;
    }

    /***************************************************************************
     * Three special filter enumeration functions:
     *    Given a tableNode of some type and then enumerate canonical filters for the node (based on the type)
     *    -- Given an renamed tablenode, enumerate canonical filters of these nodes
     *   (this enumerator will automatically figure out which filter to be enumerated based on table type)
     *************************************************************************** */

    // Generated filters are used for filtering the renamed table rt
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
        EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

        // For enumeration of named table, allow exists is always set to true.
        boolean allowExists = true;
        return FilterEnumerator.enumFiltersLR(vals, ec2.getValNodes(), ec2, allowExists);
    }

    // Generated filters are used for filtering the renamed table rt
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

        Map<String, ValType> typeMap = new HashMap<>();
        for (int i = 0; i < rt.getSchema().size(); i ++) {
            typeMap.put(rt.getSchema().get(i), rt.getSchemaType().get(i));
        }
        ec = EnumContext.extendValueBinding(ec, typeMap);

        List<Filter> filters = new ArrayList<>();

        for (int i = 0; i < tableBoundaryIndex.size() - 1; i ++) {
            List<ValNode> L = new ArrayList<>();
            for (int k = tableBoundaryIndex.get(i); k < tableBoundaryIndex.get(i + 1); k ++) {
                L.add(new NamedVal(rt.getSchema().get(k)));
            }
            for (int j = i + 1; j < tableBoundaryIndex.size() - 1; j ++) {
                List<ValNode> R = new ArrayList<>();
                for (int k = tableBoundaryIndex.get(j); k < tableBoundaryIndex.get(j+1); k ++) {
                    R.add(new NamedVal(rt.getSchema().get(k)));
                }

                boolean allowExists = false;
                filters.addAll(FilterEnumerator.enumFiltersLR(L, R, ec, allowExists));
            }
        }
        return filters;
    }

    // Generated filters are used for filtering the renamed table rt
    public static List<Filter> enumCanonicalFilterAggrNode(RenameTableNode rt, EnumContext ec) {

        if (! (rt.getTableNode() instanceof AggregationNode))
            System.out.println("[ERROR@EnumCanonicalFilters 44] " + rt.getTableNode().getClass());

        AggregationNode an =(AggregationNode) rt.getTableNode();

        int indexBoundary = an.getAggrFieldSize();

        // extend the type information to contain values inside enumcontext
        Map<String, ValType> typeMap = new HashMap<>();
        if (rt.getSchema().size() != rt.getSchemaType().size())
            System.out.println("[ERROR@EnumCanonicalFilters 61] " + rt.getSchema().size() + " ~ " + rt.getSchemaType().size());
        for (int i = 0; i < rt.getSchema().size(); i ++) {
            typeMap.put(rt.getSchema().get(i), rt.getSchemaType().get(i));
        }
        ec = EnumContext.extendValueBinding(ec, typeMap);

        // Left value can only be an aggregation target
        List<ValNode> L = new ArrayList<>();
        for (int i = indexBoundary; i < rt.getSchema().size(); i ++) {
            L.add(new NamedVal(rt.getSchema().get(i)));
        }

        // do not allow exists in aggregation result
        boolean allowExists = false;
        return FilterEnumerator.enumFiltersLR(L, ec.getValNodes(), ec, allowExists);
    }

}
