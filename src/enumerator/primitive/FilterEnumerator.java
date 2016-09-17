package enumerator.primitive;

import enumerator.context.EnumContext;
import enumerator.parameterized.EnumParamTN;
import enumerator.parameterized.InstantiateEnv;
import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.filter.*;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.NamedVal;
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

    // filters enumerated without considering L-set and R-set
    public static List<Filter> enumFiltersAll(EnumContext ec, boolean allowExists) {
        List<Filter> basicFilters = enumAtomicFilter(ec, allowExists);
        List<Filter> result = enumCompoundFilters(basicFilters, 1, ec.getMaxFilterLength());
        return result;
    }

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

                if (filters.get(i) instanceof ExistComparator && filters.get(j) instanceof ExistComparator)
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

    // enumerate only atomic filters:
    //      filters such that length == 1
    public static List<Filter> enumAtomicFilter(EnumContext ec, boolean allowExists) {
        List<Filter> atomicFilters = new ArrayList<Filter>();

        List<ValNode> valNodes = ValueEnumerator.enumValNodes(ec);

        // Enumerate VVComparators
        for (int i = 0; i < valNodes.size(); i ++) {
            for (int j = i + 1; j < valNodes.size(); j ++) {

                if (valNodes.get(i) instanceof ConstantVal
                        && valNodes.get(j) instanceof ConstantVal)
                    continue;

                ValNode l = valNodes.get(i);
                ValNode r = valNodes.get(j);

                if (l.getType(ec).equals(r.getType(ec))) {
                    if (l.getType(ec).equals(ValType.NumberVal) || l.getType(ec).equals(ValType.DateVal)) {
                        for (BiFunction<Value, Value, Boolean> func : VVComparator.getAllFunctions()) {
                            atomicFilters.add(new VVComparator(Arrays.asList(l, r), func));
                        }
                    } else {
                        atomicFilters.add(new VVComparator(Arrays.asList(l, r), VVComparator.eq));
                        atomicFilters.add(new VVComparator(Arrays.asList(l, r), VVComparator.neq));
                    }
                }
            }
        }

        // Enumerate Filter with Subquery,
        // only do this when the enum level is less than maximum level
        if (allowExists) {
            List<List<ValNode>> llvns = CombinationGenerator.genCombination(
                    ec.getValNodes(), EnumParamTN.numberOfParams);
            for (List<ValNode> vns : llvns) {
                InstantiateEnv ie = new InstantiateEnv(vns, ec);
                atomicFilters.addAll(
                        ec.getParameterizedTables().stream().map(tn -> tn.instantiate(ie))
                                .filter(t -> t.getAllHoles().size() == 0)
                                .map(tn -> new ExistComparator(tn))
                                .collect(Collectors.toList()));
                // Also enumerate not exists
                atomicFilters.addAll(
                        ec.getParameterizedTables().stream().map(tn -> tn.instantiate(ie))
                                .filter(t -> t.getAllHoles().size() == 0)
                                .map(tn -> new ExistComparator(tn, true))
                                .collect(Collectors.toList()));
            }
        }
        List<Filter> resultFilter = new ArrayList<>();
        resultFilter.addAll(atomicFilters);

        // TODO: currently neg filters are not added
        for (int i = 0; i < atomicFilters.size(); i ++) {
           // resultFilter.add(new LogicNegFilter(atomicFilters.get(i)));
        }

        return resultFilter;
    }

    // TODO: optimize by ruling out same filters
    private static List<Filter> enumAtomicFiltersLR(List<ValNode> L, List<ValNode> R, EnumContext ec, boolean allowExists) {
        // L contains all left values, and R contains all right values in a comparator
        List<Filter> atomics = new ArrayList<>();
        for (ValNode l : L) {
            for (ValNode r : R) {
                if (l.equalsToValNode(r)) continue;
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
                                .map(tn -> new ExistComparator(tn))
                                .collect(Collectors.toList()));
                // Also enumerate not exists
                atomics.addAll(
                        ec.getParameterizedTables().stream().map(tn -> tn.instantiate(ie))
                                .filter(t -> t.getAllHoles().size() == 0)
                                .map(tn -> new ExistComparator(tn, true))
                                .collect(Collectors.toList()));
            }
        }
        return atomics;
    }

}
