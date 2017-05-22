package lang.sql.dataval;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by clwang on 4/3/17.
 */
public class CombinedVal implements Value {

    Set<Value> components = new HashSet<>();

    public CombinedVal(Collection<Value> vals) {
        this.components.addAll(vals.stream().map(x -> x.duplicate()).collect(Collectors.toList()));
    }

    @Override
    public Object getVal() {
        return components;
    }

    @Override
    public Value duplicate() {
        return new CombinedVal(components);
    }

    @Override
    public ValType getValType() {
        return components.iterator().next().getValType();
    }

    public Set<Value> getComponents() { return this.components; }


    public String toString() {
        return "C[" + this.components.stream()
                .map(x -> x.toString()).reduce("", (x, y)-> x + ", " + y).substring(2) + "]";
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (Value v : this.components) {
            result = (result + v.hashCode()) % 1300127;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CombinedVal) {
            return this.components.containsAll(((CombinedVal) o).components)
                    && ((CombinedVal) o).components.containsAll(this.components);
        }
        return false;
    }

    public boolean existsAnInstantiationTo(Value value) {
        return this.components.contains(value);
    }
}
