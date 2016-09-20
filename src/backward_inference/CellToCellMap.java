package backward_inference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by clwang on 2/17/16.
 * The data structure storing a mapping between cells in tables: it maps a cell in table1 to another cell in table 2.
 */
public class CellToCellMap {

    private int maxR, maxC;
    private List<List<CellIndex>> map = new ArrayList<>();

    public List<List<CellIndex>> getMap() { return map; }
    public int maxR() { return maxR; }
    public int maxC() { return maxC; }

    public Set<Integer> rowsInvolved() {
        Set<Integer> rowsInvolved = new HashSet<>();
        for (int i = 0; i < maxR; i ++) {
            rowsInvolved.add(map.get(i).get(0).r());
        }
        return rowsInvolved;
    }

    public void initialize(int maxR, int maxC) {
        this.maxR = maxR;
        this.maxC = maxC;
        // initialize the mapping such that all values in the image are (-1, -1)
        for (int i = 0; i < maxR; i ++) {
            List<CellIndex> list = new ArrayList<>();
            for (int j = 0; j < maxC; j ++) {
                list.add(CellIndex.initDummyCellIndex());
            }
            map.add(list);
        }
    }

    // add a mapping between two cells
    public void addMap(CellIndex src, CellIndex dst) {
        if (!consistencyCheck(src, dst))
            System.err.println("[ERROR@CoordInstMap30] insert a invalid mapping.");
        map.get(src.r()).set(src.c(), dst);
    }

    // check the consistency of the map after insertion:
    //   when we try to insert a map (r, c) -> (r', c'), the insertion is consistent with the current map only if
    //     1) not exists any x, x', y' s.t. (x, c) -> (x', y') and y' != c'
    //     2) not exists any x', y, y' s.t. (r, y) -> (x', y') and x' != r'
    //   if any of these two conditions are violated, adding the map is invalid
    public boolean consistencyCheck(CellIndex src, CellIndex dst) {
        // check row
        for (int j = 0; j < maxC; j ++) {
            // in this case it is not yet initialized
            if (map.get(src.r()).get(j).isDummy())
                continue;
            if (map.get(src.r()).get(j).r() != dst.r())
                return false;
        }
        for (int i = 0; i < maxR; i ++) {
            if (map.get(i).get(src.c()).isDummy())
                continue;
            if (map.get(i).get(src.c()).c() != dst.c())
                return false;
        }
        return true;
    }

    public CellToCellMap deepCopy() {
        CellToCellMap duplicate = new CellToCellMap();
        duplicate.initialize(this.maxR, this.maxC);
        for (int i = 0; i < maxR; i ++) {
            for (int j = 0; j < maxC; j ++) {
                duplicate.map.get(i).set(j, this.map.get(i).get(j).copy());
            }
        }
        return duplicate;
    }

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
    public boolean equals(Object obj) {
        if (! (obj instanceof CellToCellMap))
            return false;

        CellToCellMap target = (CellToCellMap) obj;

        if (! (this.maxR == target.maxR && this.maxC == target.maxC))
            return false;

        for (int i = 0; i < this.maxR; i ++) {
            for (int j = 0; j < this.maxC; j ++) {
                if (! (this.map.get(i).get(j) == target.map.get(i).get(j)))
                    return false;
            }
        }

        return true;
    }

}
