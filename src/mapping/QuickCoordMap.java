package mapping;

import sql.lang.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by clwang on 2/17/16.
 */
// this is simply the data structure to represent a quick mapping map
public class QuickCoordMap {
    private int maxR, maxC;
    private List<List<List<Coordinate>>> coordMap = new ArrayList<>();

    public int maxR() { return maxR; }
    public int maxC() { return maxC; }

    // initialize the map
    public void initialize(int maxR, int maxC) {
        this.maxR = maxR;
        this.maxC = maxC;
        for (int i = 0; i < this.maxR; i ++) {
            List<List<Coordinate>> list = new ArrayList<>();
            for (int j = 0; j < this.maxC; j ++)
                list.add(new ArrayList<>());
            coordMap.add(list);
        }
    }

    public List<Coordinate> getImage(Coordinate coord) {
        return this.getImage(coord.r(), coord.c());
    }

    public List<Coordinate> getImage(int i, int j) {
        return coordMap.get(i).get(j);
    }

    // add the mapping pair u -> v into the quick map
    public void addPair(Coordinate u, Coordinate v) {
        coordMap.get(u.r()).get(u.c()).add(v);
    }

    // remove the mapping pair u -> v from the map
    // note that this operation is super slow
    public void removePair(Coordinate u, Coordinate v) {
        coordMap.get(u.r()).get(u.c()).remove(v);
    }

    public Map<Coordinate, List<Coordinate>> toMap() {
        Map<Coordinate, List<Coordinate>> map = new HashMap<>();
        for (int i = 0; i < coordMap.size(); i ++) {
            for (int j = 0; j < coordMap.get(i).size(); j ++) {
                map.put(new Coordinate(i, j), coordMap.get(i).get(j));
            }
        }
        return map;
    }
}