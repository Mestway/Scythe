package backward_inference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by clwang on 2/17/16.
 * This is the data structure that maps a cell to a set of cells (cells are represented using their indexes)
 */
public class CellToExpSetMap extends CellMapping<Set<CellIndexExp>>{

    // initialize the map
    CellToExpSetMap(int maxR, int maxC) {
        super(maxR, maxC, () -> new HashSet<>());
    }

    // add the mapping pair u -> v into the quick map
    public void addPair(CellIndex u, CellIndexExp v) {
        this.map.get(u.r()).get(u.c()).add(v);
    }

    // remove the mapping pair u -> v from the map
    // TODO: this operation is super slow
    public void removePair(CellIndex u, CellIndexExp v) {
        this.map.get(u.r()).get(u.c()).remove(v);
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof CellToExpSetMap))
            return false;
        CellToExpSetMap target = (CellToExpSetMap) obj;
        if (! (this.maxR == target.maxR && this.maxC == target.maxC))
            return false;
        for (int i = 0; i < this.maxR; i ++) {
            for (int j = 0; j < this.maxC; j ++) {
                if (! map.get(i).get(j).equals(target.map.get(i).get(j)))
                    return false;
            }
        }
        return true;
    }

    @Override
    boolean consistencyCheck(CellIndex src, Set<CellIndexExp> dst) {
        return true;
    }

    @Override
    public CellToExpSetMap copy() {
        CellToExpSetMap ccsm = new CellToExpSetMap(maxR, maxC);
        ccsm.map = new ArrayList<>();
        for (List<Set<CellIndexExp>> l : this.map) {
            List<Set<CellIndexExp>> tmp = new ArrayList();
            for (Set<CellIndexExp> ci : l) {
                Set<CellIndexExp> s = new HashSet<>();
                for(CellIndexExp c : ci)
                    s.add(c.copy());
                tmp.add(s);
            }
            ccsm.map.add(tmp);
        }
        return ccsm;
    }
}