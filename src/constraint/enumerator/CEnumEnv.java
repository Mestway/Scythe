package constraint.enumerator;

import constraint.lang.filter.*;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.filter.VVComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/7/16.
 */
public class CEnumEnv {

    // initialized values
    Table input;
    Table output;
    List<Value> constants = new ArrayList<>();
    List<BiFunction<Value, Value, Boolean>> comparators = new ArrayList<>();
    List<Function<List<Value>, Value>> aggrFuncs = new ArrayList<>();
    int maxFilterLength = 3;

    // dynamic maintained values
    boolean parametersEnumerated = false;
    List<Parameter> parameters = new ArrayList<>();
    boolean comparatorsEnumerated = false;
    List<CBiComparator> primitives = new ArrayList<>();
    List<CFilter> filters = new ArrayList<>();

    public CEnumEnv(Table input,
                    Table output,
                    List<Value> constants,
                    List<Function<List<Value>, Value>> aggrFuncs) {
        this.input = input;
        this.output = output;
        this.constants = constants;
        this.aggrFuncs = aggrFuncs;
        this.comparators.addAll(
                Arrays.asList(VVComparator.ge, VVComparator.eq, VVComparator.gt,
                        VVComparator.le, VVComparator.lt, VVComparator.neq));
    }

    public void enumerateParameter() {
        parameters.addAll(input.getMetadata().stream().map(InParameter::new).collect(Collectors.toList()));
        parameters.addAll(output.getMetadata().stream().map(OutParameter::new).collect(Collectors.toList()));
        parameters.addAll(constants.stream().map(ConstParameter::new).collect(Collectors.toList()));
        parametersEnumerated = true;
    }

    public void enumerateCBiComparator() {
        // comparators using parameters
        if (!parametersEnumerated)
            enumerateParameter();
        for (int i = 0; i < parameters.size(); i ++) {
            Parameter p1  = parameters.get(i);
            for (int j = i + 1; j < parameters.size(); j ++) {
                Parameter p2 = parameters.get(j);
                if (p1.equals(p2)) continue;
                if (!p1.getType(input, output).equals(p2.getType(input, output)))
                    continue;
                for (BiFunction<Value, Value, Boolean> c : comparators) {
                    primitives.add(new CBiComparator(p1, p2, c));
                }
            }
        }
        comparatorsEnumerated = true;
    }
    public List<CBiComparator> getPrimitiveComparators() { return primitives; }

    public void enumerateFilters() {

        if (!comparatorsEnumerated)
            enumerateCBiComparator();

        List<CFilter> filters = new ArrayList<>();

        for (int depth = 1; depth <= maxFilterLength; depth ++) {
            if (depth == 1) {
                for (CBiComparator cbc : primitives) {
                    filters.add(new CFilter(cbc));
                }
                continue;
            }
            List<CFilter> nextIteration = new ArrayList<>();
            for (CFilter f : filters) {
                for (CBiComparator p : primitives) {
                    boolean conflict = false;
                    for (CBiComparator fp : f.getComparators()) {
                        if (CBiComparator.conflict(p, fp))
                            conflict = true;
                    }
                    if (conflict == true) continue;
                    List<CBiComparator> newFilter = new ArrayList<>();
                    newFilter.addAll(f.getComparators());
                    newFilter.add(p);
                    nextIteration.add(new CFilter(newFilter));
                }
            }
            filters = nextIteration;
        }
        this.filters = filters;
    }
    public List<CFilter> getFilters() { return this.filters; }

}
