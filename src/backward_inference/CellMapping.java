package backward_inference;

import util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The data structure storing a mapping between cells in tables: it maps a cell in a table to objects
 * (possibly related to another table).
 * Created by clwang on 5/22/17.
 */
public abstract class CellMapping<T> {

    protected int maxR, maxC;
    protected List<List<T>> map = new ArrayList<>();
    protected List<CellIndex> keys = new ArrayList<>();

    public int maxR() { return maxR; }
    public int maxC() { return maxC; }

    /**
     * Create a CellMapping instance
     * @param maxRow the total number of rows in the table
     * @param maxCol the total number of columns in the table
     * @param defaultValueGenerator A supplier that produces default values in the map
     *                              It is a supplier since we want to avoid aliases
     */
    public CellMapping(int maxRow, int maxCol, Supplier<T> defaultValueGenerator) {
        this.maxR = maxRow;
        this.maxC = maxCol;

        // initialize the map, all mapping image are set to null upon initialization
        for (int r = 0; r < maxRow; r ++) {
            List<T> list = new ArrayList<>();
            for (int c = 0; c < maxCol; c ++) {
                list.add(defaultValueGenerator.get());
                keys.add(new CellIndex(r, c));
            }
            map.add(list);
        }
    }

    // add a mapping between two cells
    public void insert(CellIndex src, T dst) {
        if (! consistencyCheck(src, dst))
            System.err.println("[ERROR@CellMapping43] insert a invalid mapping.");
        map.get(src.r()).set(src.c(), dst);
    }

    public T getImage(CellIndex src) { return this.map.get(src.r()).get(src.c()); }
    public T getImage(int r, int c) { return this.map.get(r).get(c); }

    public List<CellIndex> keys() { return keys; }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < maxR; i ++) {
            for (int j = 0; j < maxC; j ++) {
                s += "(" + i + "," + j + ")" + "->" + this.map.get(i).get(j).toString() + "  ";
            }
            s += "\n";
        }
        return s;
    }

    @Override
    public int hashCode() {
        int hashResult = this.maxC * 13 + this.maxR;
        for (int i = 0; i < this.maxR; i ++) {
            for (int j = 0; j < this.maxC; j ++) {
                hashResult += (i * 13 + j) * this.map.get(i).get(j).hashCode() % 15487139;
            }
        }
        return hashResult;
    }

    @Override
    public abstract boolean equals(Object obj);
    // check whether a newly inserted mapping is consistent with existing ones
    abstract boolean consistencyCheck(CellIndex src, T dst);

    /**
     * deep copying the current mapping
     * @return a fresh mapping copied from the current one
     */
    abstract CellMapping<T> copy();
}
