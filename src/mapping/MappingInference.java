package mapping;

import sql.lang.DataType.Value;
import sql.lang.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by clwang on 2/17/16.
 */
public class MappingInference {

    // program convention in this class: i loops over rows and j loops over columns.

    Table input, output;
    int maxR, maxC;
    // the mapping between variables
    QuickCoordMap map = new QuickCoordMap();

    public void buildMapping(Table in, Table out) {

        if (in.getContent().size() == 0 || out.getContent().size() == 0) {
            System.err.println("[Error@MappingInference22]In/Out table is empty.");
        }

        this.maxR = out.getContent().size();
        this.maxC = out.getContent().get(0).getValues().size();
        map.initialize(maxR, maxC);

        this.input = in; this.output = out;
        for (int i = 0; i < output.getContent().size(); i ++) {
            for (int j = 0; j < output.getContent().get(i).getValues().size(); j ++) {
                Value v = output.getContent().get(i).getValue(j);
                for (int k = 0; k < input.getContent().size(); k ++) {
                    for (int l = 0; l < input.getContent().get(k).getValues().size(); l ++) {
                        Value v2 = input.getContent().get(k).getValue(l);
                        if (v.equals(v2)) {
                            map.addPair(new Coordinate(i,j), new Coordinate(k,l));
                        }
                    }
                }
            }
        }
    }

    // get the image of a key from a map
    private static <T> List<T> getMappingImg (Map<T, List<T>> map, T u) {
        for (Map.Entry<T, List<T>> entry : map.entrySet()) {
            if (entry.getKey().equals(u)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // think carefully, we can either refine them into an approximate
    // set or instantiate them fully into concrete mappings,
    // how does this affect the result?
    public void refineMapping() {
        boolean stable = false;
        while (!stable) {
            stable = true;
            for (int i = 0; i < map.maxR(); i ++) {
                for (int j = 0; j < map.maxC(); j ++) {
                    Coordinate src = new Coordinate(i, j);
                    List<Coordinate> toRemove = new ArrayList<>();
                    for (Coordinate coord : map.getImage(src)) {
                        if (!validCheck(src, coord)) {
                            toRemove.add(coord);
                            stable = false;
                        }
                    }
                    for (Coordinate rm : toRemove) {
                        map.removePair(src, rm);
                    }
                }
            }
        }
    }

    /* a map pair (r,c) -> (r',c') is valid, if
       forall v in [0,...,outputC], exists v', s.t. (r',v') in Img((r,v))
       forall u in [0,...,outputR], exists u', s.t. (u',c') in Img((u,c))
       if the condition above is satisfied, we will not touch the code,
       otherwise we will remove the entry from the image. */
    private boolean validCheck(Coordinate src, Coordinate dst) {
        int r = src.r(), c = src.c();
        int rPrim = dst.r(), cPrim = dst.c();

        // corresponding to check 1
        for (int v = 0; v < map.maxC(); v ++) {
            boolean exists = false;
            for (Coordinate coord : map.getImage(new Coordinate(r, v))) {
                if (coord.r() == rPrim)
                    exists = true;
            }
            if (exists == false)
                return false;
        }

        // corresponding to check 2
        for (int u = 0; u < map.maxR(); u ++) {
            boolean exists = false;
            for (Coordinate coord : map.getImage(new Coordinate(u, c))) {
                if (coord.c() == cPrim)
                    exists = true;
            }
            if (!exists) return false;
        }
        return true;
    }

    // a mapping instance will map each coordinate in output table to a coordinate in input table
    // the mapping instance is generated through the refined map
    // in the result table, each list in quick coord table should have length 1.
    public List<CoordInstMap> genMappingInstances() {
        CoordInstMap instance = new CoordInstMap();
        instance.initialize(maxR, maxC);
        List<CoordInstMap> resultCollector = new ArrayList<>();
        dfsMappingSearch(0, 0, maxR, maxC, instance, resultCollector, this.map);
        return resultCollector;
    }

    private void dfsMappingSearch(
            int i, int j,
            int maxI, int maxJ,
            CoordInstMap currentMap,
            List<CoordInstMap> resultCollector,
            QuickCoordMap candidatePool) {

        // when dfs reaches its goal
        if (i >= maxI || j >= maxJ) {
            resultCollector.add(currentMap);
            return;
        }

        for (Coordinate coord : candidatePool.getImage(i,j)) {
            if (currentMap.validCheck(new Coordinate(i,j), coord)) {
                CoordInstMap nextMap = currentMap.deepCopy();
                nextMap.addMap(new Coordinate(i,j), coord);
                int nextI = i, nextJ = j + 1;
                if (nextJ >= maxJ) {
                    nextJ = nextJ % maxJ;
                    nextI ++;
                }
                dfsMappingSearch(
                        nextI, nextJ,
                        maxI, maxJ,
                        nextMap, resultCollector, candidatePool);
            }
        }
    }

    public static Map<Integer, Integer> lineMappingInference(CoordInstMap cim) {
        Map<Integer, Integer> lineMap = new HashMap<>();
        for (int i = 0; i < cim.maxR(); i ++) {
            lineMap.put(cim.getMap().get(i).get(0).r(), i);
        }
        return lineMap;
    }

    @Override
    public String toString() {
        String s = "";
        for (Map.Entry<Coordinate, List<Coordinate>> t : map.toMap().entrySet()) {
            String listString = t.getValue().stream()
                    .map(u -> u.toString() + ":" + input.getContent().get(u.r()).getValue(u.c()))
                    .reduce("", (x, y)-> x.toString() + ","+ y.toString()).trim();
            s += t.getKey().toString() + " : " + output.getContent().get(t.getKey().r()).getValue(t.getKey().c())
                    + " -> " + " ["
                    + listString.substring(listString.indexOf("(")) + "]\n";
        }
        return s;
    }

}