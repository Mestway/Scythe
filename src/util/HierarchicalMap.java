package util;

import sql.lang.Table;

import java.util.*;

/**
 * The data structure can only be used upon tables
 * Created by clwang on 3/26/16.
 */
public class HierarchicalMap<K, V> implements Map<K, V> {

    TreeMap<Integer, Map<K, V>> map = new TreeMap<>();

    @Override
    public int size() {
        return map.entrySet().stream()
                .map(e -> e.getValue().size()).reduce(0, (x, y) -> x + y);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof Table) {
            if (map.containsKey(((Table) key).getSizeValue())) {
                Map subMap = map.get(((Table) key).getSizeValue());
                if (subMap.containsKey(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        System.err.println("[HierachicalMap 41] This method is not implemented.");
        return false;
    }

    @Override
    public V get(Object key) {
        if (key instanceof Table ) {
            if (this.containsKey(key))
                return map.get(((Table) key).getSizeValue()).get(key);
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        if (key instanceof Table) {
            if (map.containsKey(((Table) key).getSizeValue())) {
                map.get(((Table) key).getSizeValue()).put(key, value);
                return value;
            } else {
                Map<K, V> subMap = new HashMap<>();
                subMap.put(key, value);
                map.put(((Table) key).getSizeValue(), subMap);
                return value;
            }
        }
        System.err.println("[HierachicalMap 59] key should be a table.");
        return null;
    }

    @Override
    public V remove(Object key) {
        System.err.println("[HierachicalMap 61] This method is not implemented.");
        return null;
    }

    @Override
    public void putAll(Map m) {
        System.err.println("[HierachicalMap 71] This method is not implemented.");
    }

    @Override
    public void clear() {
        System.err.println("[HierachicalMap 73] This method is not implemented.");
    }

    @Override
    public Set keySet() {
        Set<K> keyset = new HashSet<>();
        for (Entry<Integer, Map<K, V>> e : map.entrySet()) {
            keyset.addAll(e.getValue().keySet());
        }
        return keyset;
    }

    @Override
    public Collection values() {
        System.err.println("[HierachicalMap 96] This method is not implemented.");
        return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> eSet = new HashSet<>();
        for (Entry<Integer, Map<K, V>> e : map.entrySet()) {
            eSet.addAll(e.getValue().entrySet());
        }
        return eSet;
    }
}
