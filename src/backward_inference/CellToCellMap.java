package backward_inference;

import util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 2/17/16.
 * The data structure storing a mapping between cells in tables: it maps a cell in table1 to another cell in table 2.
 */
public class CellToCellMap extends CellMapping<CellIndex> {

    public CellToCellMap(int maxR, int maxC) {
        super(maxR, maxC, () -> CellIndex.dummyCellIndex());
    }

    // check the consistency of the map after insertion:
    //   when we try to insert a map (r, c) -> (r', c'), the insertion is consistent with the current map only if
    //     1) not exists any x, x', y' s.t. (x, c) -> (x', y') and y' != c'
    //     2) not exists any x', y, y' s.t. (r, y) -> (x', y') and x' != r'
    //   if any of these two conditions are violated, adding the map is invalid
    @Override
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

    @Override
    public CellToCellMap copy() {
        CellToCellMap duplicate = new CellToCellMap(this.maxR, this.maxC);
        for (int i = 0; i < maxR; i ++) {
            for (int j = 0; j < maxC; j ++) {
                duplicate.map.get(i).set(j, this.map.get(i).get(j).copy());
            }
        }
        return duplicate;
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

    /**
     * Obtain the column mapping indicated in this cell mapping
     * @return a list indicating the mapping:
     * result[i] = c indicates that column i in the source table is mapped to column c in the target table
     */
    public List<Integer> getColumnMapping() {
        List<Integer> result = new ArrayList<>();
        for (int c = 0; c < this.maxC; c ++) {
            result.add(this.map.get(0).get(c).c());
        }
        return result;
    }

}
