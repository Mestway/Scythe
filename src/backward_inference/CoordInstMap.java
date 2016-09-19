package backward_inference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by clwang on 2/17/16.
 */
public class CoordInstMap {

    private int maxR, maxC;
    private List<List<Coordinate>> map = new ArrayList<>();

    public List<List<Coordinate>> getMap() { return map; }
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
            List<Coordinate> list = new ArrayList<>();
            for (int j = 0; j < maxC; j ++) {
                list.add(Coordinate.genDummy());
            }
            map.add(list);
        }
    }

    public void addMap(Coordinate src, Coordinate dst) {
        if (!validCheck(src, dst))
            System.err.println("[ERROR@CoordInstMap30] insert a invalid mapping.");
        map.get(src.r()).set(src.c(), dst);
    }

    public boolean validCheck(Coordinate src, Coordinate dst) {
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

    public CoordInstMap deepCopy() {
        CoordInstMap duplicate = new CoordInstMap();
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

}
