package util;

/**
 * Created by clwang on 5/20/16.
 */
public class Triple<U,V, W> {
    Pair<U, Pair<V, W>> triple;

    public Triple(U u, V v, W w) {
        triple = new Pair(u, new Pair(v, w));
    }
    public U first() {
        return triple.getKey();
    }
    public V second() {
        return triple.getValue().getKey();
    }
    public W third() {
        return triple.getValue().getValue();
    }

    @Override
    public int hashCode() {
        return triple.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Triple) {
            return triple.equals(((Triple) obj).triple);
        }
        return false;
    }
}
