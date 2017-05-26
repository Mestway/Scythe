package backward_inference;

import util.Pair;
import util.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by clwang on 2/17/16.
 * The data structure storing a mapping between cells in tables: it maps a cell in table1 to another cell in table 2.
 */
public class CellToExpMap extends CellMapping<CellIndexExp> {

    public CellToExpMap(int maxR, int maxC) {
        super(maxR, maxC, () -> null);
    }

    // Similar to the one in CellToCellMap
    @Override
    public boolean consistencyCheck(CellIndex src, CellIndexExp newEntry) {
        // check row consistency
        for (int j = 0; j < maxC; j ++) {
            CellIndex currentCell = new CellIndex(src.r(), j);
            CellIndexExp existingEntry = this.getImage(currentCell);

            // in this case it is not yet initialized
            if (existingEntry == null)
                continue;

            if (! CellIndexExp.pairWiseConsistency(
                    new Pair<>(currentCell, existingEntry),
                    new Pair<>(src, newEntry)))
                return false;
        }
        for (int i = 0; i < maxR; i ++) {

            // check column consistency
            CellIndex currentCell = new CellIndex(i, src.c());
            CellIndexExp existingEntry = this.getImage(currentCell);

            if (existingEntry == null)
                continue;

            if (! CellIndexExp.pairWiseConsistency(
                    new Pair<>(currentCell, existingEntry),
                    new Pair<>(src, newEntry)))
                return false;
        }
        return true;
    }

    @Override
    public CellToExpMap copy() {
        CellToExpMap duplicate = new CellToExpMap(this.maxR, this.maxC);
        for (int i = 0; i < maxR; i ++) {
            for (int j = 0; j < maxC; j ++) {
                duplicate.map.get(i).set(j, this.map.get(i).get(j).copy());
            }
        }
        return duplicate;
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof CellToExpMap))
            return false;

        CellToExpMap target = (CellToExpMap) obj;
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
