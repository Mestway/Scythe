package lang.sql.dataval;

import lang.sql.ast.abstable.AbsAggrNode;
import util.CombinationGenerator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by clwang on 4/3/17.
 */
public class LazyAggrVal implements Value {

    // This value represents a list of values,

    Function<List<Value>, Value> f;
    // note that we cannot use set to store this, since values may be used up to many times by the aggregation function
    List<Value> components = new ArrayList<>();

    public LazyAggrVal(List<Value> vals, Function<List<Value>, Value> f) {
        this.components.addAll(vals.stream().map(x -> x.duplicate()).collect(Collectors.toList()));
        this.f = f;
    }

    @Override
    public Object getVal() {
        return components;
    }

    @Override
    public Value duplicate() {
        return new LazyAggrVal(components, f);
    }

    @Override
    public ValType getValType() {
        return f.apply(components).getValType();
    }

    public List<Value> getComponents() { return this.components; }

    public CombinedVal instantiateToCombinedVal() {

        if (f == AbsAggrNode.AggrMax || f == AbsAggrNode.AggrMin || f == AbsAggrNode.AggrFirst) {
            return new CombinedVal(components.stream().collect(Collectors.toSet()));
        }

        if (f == AbsAggrNode.AggrCount) {
            return new CombinedVal(IntStream.range(0, this.components.size())
                    .mapToObj(i -> new NumberVal(i)).collect(Collectors.toSet()));
        }

        if (f == AbsAggrNode.AggrCountDistinct) {
            return new CombinedVal(IntStream.range(0, this.components.stream().collect(Collectors.toSet()).size())
                    .mapToObj(i -> new NumberVal(i)).collect(Collectors.toSet()));
        }

        if (f == AbsAggrNode.AggrSum || f == AbsAggrNode.AggrAvg) {

            Set<Value> combResult = new HashSet<>();

            Map<Value, Integer> valMultiplicity = new HashMap<>();
            for (Value v : this.getComponents()) {
                if (! valMultiplicity.containsKey(v))
                    valMultiplicity.put(v, 0);
                valMultiplicity.replace(v, valMultiplicity.get(v) + 1);
            }
            Vector<Double> values = new Vector<>();
            List<Integer> multiplicityBound = new ArrayList<>();
            for (Map.Entry<Value, Integer> e : valMultiplicity.entrySet()) {
                if (e.getKey() instanceof NumberVal) {
                    values.add(((NumberVal) e.getKey()).getVal());
                    multiplicityBound.add(e.getValue());
                }
            }
            List<Vector<Integer>> allMult = CombinationGenerator.genAllVectorLE(multiplicityBound);
            for (Vector<Integer> vec : allMult) {
                Double temp = 0.;
                for (int i = 0; i < vec.size(); i ++) {
                    temp += vec.get(i) * values.get(i);
                }
                if (f == AbsAggrNode.AggrSum) {
                    combResult.add(new NumberVal(temp));
                } else if (f == AbsAggrNode.AggrAvg) {
                    combResult.add(new NumberVal(temp / vec.stream().reduce((x, y)-> x + y).get()));
                }
            }

            return new CombinedVal(combResult);
        }

        // this one is dumb
        return new CombinedVal(CombinationGenerator.genCombination(components)
                                .stream()
                                .map(l -> f.apply(l))
                                .distinct()
                                .collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + AbsAggrNode.FuncName(f) + ") [" + this.components.stream()
                .map(x -> x.toString()).reduce("", (x, y)-> x + ", " + y).substring(2) + "]";
    }

    @Override
    public int hashCode() {
        int result = this.f.hashCode();
        for (Value v : this.components) {
            result = (result + v.hashCode()) % 1300127;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LazyAggrVal) {
            return this.components.containsAll(((LazyAggrVal) o).components)
                    && ((LazyAggrVal) o).components.containsAll(this.components) && this.f.equals(((LazyAggrVal) o).f);
        }
        return false;
    }

    // this is a simple but dumb implementation of the approach
    public boolean existsAnInstantiationTo(Value val) {
        return this.instantiateToCombinedVal().existsAnInstantiationTo(val);
    }

}
