package backward_inference;

import java.util.*;

/**
 * Created by clwang on 2/17/16.
 * This is the data structure that maps a cell to a set of cells (cells are represented using their indexes)
 */
public class CellToCellSetMap {
    private int maxR, maxC;
    private List<List<Set<CellIndex>>> coordMap = new ArrayList<>();

    public int maxR() { return maxR; }
    public int maxC() { return maxC; }

    // initialize the map
    public void initialize(int maxR, int maxC) {
        this.maxR = maxR;
        this.maxC = maxC;
        for (int i = 0; i < this.maxR; i ++) {
            List<Set<CellIndex>> list = new ArrayList<>();
            for (int j = 0; j < this.maxC; j ++)
                list.add(new HashSet<>());
            coordMap.add(list);
        }
    }

    public Set<CellIndex> getImage(CellIndex coord) {
        return this.getImage(coord.r(), coord.c());
    }
    public Set<CellIndex> getImage(int i, int j) {
        return coordMap.get(i).get(j);
    }

    // add the mapping pair u -> v into the quick map
    public void addPair(CellIndex u, CellIndex v) {
        coordMap.get(u.r()).get(u.c()).add(v);
    }

    // remove the mapping pair u -> v from the map
    // note that this operation is super slow
    public void removePair(CellIndex u, CellIndex v) {
        coordMap.get(u.r()).get(u.c()).remove(v);
    }

    // convert this map to a normal hashmap
    public Map<CellIndex, Set<CellIndex>> toMap() {
        Map<CellIndex, Set<CellIndex>> map = new HashMap<>();
        for (int i = 0; i < coordMap.size(); i ++) {
            for (int j = 0; j < coordMap.get(i).size(); j ++) {
                map.put(new CellIndex(i, j), coordMap.get(i).get(j));
            }
        }
        return map;
    }

    @Override
    public int hashCode() {
        int hashResult = this.maxC * 13 + this.maxR;
        for (int i = 0; i < this.maxR; i ++) {
            for (int j = 0; j < this.maxC; j ++) {
                hashResult += (i * 13 + j) * this.coordMap.get(i).get(j).hashCode() % 15487139;
            }
        }
        return hashResult;
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof CellToCellSetMap))
            return false;

        CellToCellSetMap target = (CellToCellSetMap) obj;

        if (! (this.maxR == target.maxR && this.maxC == target.maxC))
            return false;

        for (int i = 0; i < this.maxR; i ++) {
            for (int j = 0; j < this.maxC; j ++) {
                if (! coordMap.get(i).get(j).equals(target.coordMap.get(i).get(j)))
                    return false;
            }
        }

        return true;
    }

    public CellToCellSetMap deepCopy() {
        CellToCellSetMap ccsm = new CellToCellSetMap();
        ccsm.maxC = this.maxC;
        ccsm.maxR = this.maxR;
        ccsm.coordMap = new ArrayList<>();
        for (List<Set<CellIndex>> l : this.coordMap) {
            List<Set<CellIndex>> tmp = new ArrayList();
            for (Set<CellIndex> ci : l) {
                Set<CellIndex> s = new HashSet<>();
                for(CellIndex c : ci)
                    s.add(c.copy());
                tmp.add(s);
            }
            ccsm.coordMap.add(tmp);
        }
        return ccsm;
    }
}