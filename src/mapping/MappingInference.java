package mapping;

import enumerator.Constraint;
import enumerator.context.EnumContext;
import enumerator.primitive.FilterEnumerator;
import enumerator.parameterized.EnumParamTN;
import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.CombinationGenerator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A class used to inference possible mappings from input to output table.
 * Created by clwang on 2/17/16.
 */
public class MappingInference {

    // program convention in this class: i loops over rows and j loops over columns.

    Table input, output;
    int maxR, maxC;
    // the mapping between variables
    QuickCoordMap map = new QuickCoordMap();

    private MappingInference() {};

    public static MappingInference buildMapping(Table in, Table out) {

        MappingInference mi = new MappingInference();

        if (in.getContent().size() == 0 || out.getContent().size() == 0) {
            System.err.println("[Error@MappingInference22]In/Out table is empty.");
        }

        mi.maxR = out.getContent().size();
        mi.maxC = out.getContent().get(0).getValues().size();
        mi.map.initialize(mi.maxR, mi.maxC);

        mi.input = in; mi.output = out;
        for (int i = 0; i < mi.output.getContent().size(); i ++) {
            for (int j = 0; j < mi.output.getContent().get(i).getValues().size(); j ++) {
                Value v = mi.output.getContent().get(i).getValue(j);
                for (int k = 0; k < mi.input.getContent().size(); k ++) {
                    for (int l = 0; l < mi.input.getContent().get(k).getValues().size(); l ++) {
                        Value v2 = mi.input.getContent().get(k).getValue(l);
                        if (v.equals(v2)) {
                            mi.map.addPair(new Coordinate(i,j), new Coordinate(k,l));
                        }
                    }
                }
            }
        }
        mi.refineMapping();
        return mi;
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
    // Refine mapping is an approximate process, as it is simply trying to remove invalid mappings
    // TODO: refind mapping is now integrated into building process
    private void refineMapping() {
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

    // We can have some interesting property here:
    //      The column inference result can be obtained even if we only apply the inference on the first row
    // the result is a list of set:
    //      result.get(c) represents the image sef ot c in the mapping
    public List<Set<Integer>> genColumnMappingInstances() {
        List<Set<Integer>> columnMapping = new ArrayList<>();
        for (int c = 0; c < maxC; c ++) {
            Set<Integer> destination = new HashSet<>();
            for (int i = 0; i < map.getImage(0, c).size(); i ++) {
                destination.add(map.getImage(0, c).get(i).c());
            }
            columnMapping.add(destination);
        }
        return columnMapping;
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

    // a variant version of genMapping Instances,
    // which requires some of the mapping images from left of the barrier while some other from right of the barrier.
    public List<CoordInstMap> genMappingInstancesWColumnBarrier(int columnBarrier) {

        List<Set<Integer>> columnMapping = this.genColumnMappingInstances();
        List<List<Integer>> listRepColMapping = new ArrayList<>();
        for (int i = 0; i < columnMapping.size(); i ++) {
            listRepColMapping.add(columnMapping.get(i).stream().collect(Collectors.toList()));
        }

        List<List<Integer>> targetsToSearch = CombinationGenerator.rotateList(listRepColMapping);

        List<CoordInstMap> resultCollector = new ArrayList<>();
        for (List<Integer> target : targetsToSearch) {
            boolean left = false;
            boolean right = false;
            for (Integer i : target) {
                if (i < columnBarrier) left = true;
                if (i >= columnBarrier) right = true;
            }

            if (! (left && right))
                continue;

            CoordInstMap instance = new CoordInstMap();
            instance.initialize(maxR, maxC);
            dfsMappingSearchWithFixedColumns(0, 0, maxR, maxC, instance, resultCollector, this.map, target);
        }

        return resultCollector;
    }

    // in this version
    private void dfsMappingSearchWithFixedColumns(
            int i, int j,
            int maxI, int maxJ,
            CoordInstMap currentMap,
            List<CoordInstMap> resultCollector,
            QuickCoordMap candidatePool,
            List<Integer> columnRestriction) {

        // when dfs reaches its goal
        if (i >= maxI || j >= maxJ) {
            resultCollector.add(currentMap);
            return;
        }

        for (Coordinate coord : candidatePool.getImage(i,j)) {
            if (coord.c() != columnRestriction.get(j))
                continue;

            if (currentMap.validCheck(new Coordinate(i,j), coord)) {
                CoordInstMap nextMap = currentMap.deepCopy();
                nextMap.addMap(new Coordinate(i,j), coord);
                int nextI = i, nextJ = j + 1;
                if (nextJ >= maxJ) {
                    nextJ = nextJ % maxJ;
                    nextI ++;
                }
                dfsMappingSearchWithFixedColumns(
                        nextI, nextJ,
                        maxI, maxJ,
                        nextMap, resultCollector,
                        candidatePool, columnRestriction);
            }
        }
    }

    // key: a row number in the output table
    // value: a row number in the input table
    public static Map<Integer, Integer> rowMappingInference(CoordInstMap cim) {
        Map<Integer, Integer> rowMap = new HashMap<>();
        for (int i = 0; i < cim.maxR(); i ++) {
            rowMap.put(i, cim.getMap().get(i).get(0).r());
        }
        return rowMap;
    }

    public static Map<Integer, Integer> columnMappingInference(CoordInstMap cim) {
        Map<Integer, Integer> columnMap = new HashMap<>();
        for (int j = 0; j < cim.maxC(); j ++) {
            columnMap.put(j, cim.getMap().get(0).get(j).c());
        }
        return columnMap;
    }

    public static List<BitSet> bulkBitEncodingFilter(Table table, List<Filter> filters) {

        List<BitSet> encodingList = new ArrayList<>();

        Map<Filter, Integer> filterIndex = new HashMap<>();
        for (int i = 0; i < filters.size(); i ++) {
            encodingList.add(new BitSet(table.getContent().size()));
            filterIndex.put(filters.get(i), i);
        }


        for (int i = 0; i < table.getContent().size(); i ++)
            table.getContent().get(i).index = i;

        table.getContent().parallelStream().forEach(tr -> {
            Map<String, Value> rowBinding = new HashMap<String, Value>();
            // extend the evaluation environment for this column
            if (! table.getName().equals("anonymous")) {
                for (int k = 0; k < table.getMetadata().size(); k ++) {
                    rowBinding.put(
                            table.getName() + "." + table.getMetadata().get(k),
                            tr.getValue(k));
                }
            } else {
                for (int k = 0; k < table.getMetadata().size(); k ++) {
                    rowBinding.put(table.getMetadata().get(k), tr.getValue(k));
                }
            }

            Environment env = new Environment().extend(rowBinding);
            filters.parallelStream().forEach(f -> {
                try {
                    if (f.filter(env))
                        encodingList.get(filterIndex.get(f)).set(tr.index);
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            });
        });
        return encodingList;
    }

    public static BitSet bitEncodingFilter(Table table, Filter f) {
        BitSet encoding = new BitSet(table.getContent().size());

        for (int i = 0; i < table.getContent().size(); i ++) {
            TableRow tr = table.getContent().get(i);

            Map<String, Value> rowBinding = new HashMap<String, Value>();
            // extend the evaluation environment for this column
            if (table.getName().equals("anonymous") == false) {
                for (int k = 0; k < table.getMetadata().size(); k ++) {
                    rowBinding.put(table.getName() + "." + table.getMetadata().get(k), tr.getValue(k));
                }
            } else {
                for (int k = 0; k < table.getMetadata().size(); k ++) {
                    rowBinding.put(table.getMetadata().get(k), tr.getValue(k));
                }
            }

            Environment env = new Environment().extend(rowBinding);
            try {
                if (f.filter(env)) {
                    encoding.set(i);
                }
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }
        return encoding;
    }

    public static Map<BitSet, List<Filter>> filterMemoization(Table table, List<Filter> filters) {
        Map<BitSet, List<Filter>> bitFilterMap= new HashMap<>();
        System.out.println("I'm Here");
        System.out.println("Filter Size: " + filters.size());

        // Encoding method 1: foreach filter, do encoding and add them to the map
        filters.parallelStream().forEach(f -> {
            BitSet encoding = bitEncodingFilter(table, f);
            if (!bitFilterMap.containsKey(encoding)) {
                List<Filter> list = new ArrayList<>();
                list.add(f);
                bitFilterMap.put(encoding, list);
            } else {
                bitFilterMap.get(encoding).add(f);
            }
        });

        // Encoding method 2: for each row, encoding all filters
        /*
        List<BitSet> encodings = bulkBitEncodingFilter(table, filters);
        //List<BitSet> encodings = filters.stream().parallel().map(f -> bitEncodingFilter(table, f)).collect(Collectors.toList());
        for (int i = 0; i < filters.size(); i ++) {
            Filter f = filters.get(i);
            BitSet encoding = encodings.get(i); //
             //BitSet encoding= bitEncodingFilter(table, f);
            if (!bitFilterMap.containsKey(encoding)) {
                List<Filter> list = new ArrayList<>();
                list.add(f);
                bitFilterMap.put(encoding, list);
            } else {
                bitFilterMap.get(encoding).add(f);
            }
        }
        */

        System.out.println("Value Size: " + bitFilterMap.entrySet().size());
        for (Map.Entry<BitSet, List<Filter>> k : bitFilterMap.entrySet()) {
            System.out.println("#" + k.getValue().size() + " : " + k.getKey().toString());
        }

        return bitFilterMap;
    }

    @Override
    public String toString() {
        String s = "";
        for (Map.Entry<Coordinate, List<Coordinate>> t : map.toMap().entrySet()) {
            String listString = t.getValue().stream()
                    .map(u -> u.toString())
                    .reduce("", (x, y)-> x.toString() + ","+ y.toString()).trim();
            s += t.getKey().toString()
                    + " -> " + " ["
                    + listString.substring(listString.indexOf("(")) + "]\n";
        }
        return s;
    }

    // TODO: PLEASE DOUBlE CHECK
    public boolean isValidMapping() {
        for (int i = 0; i < map.maxR(); i ++)
            for (int j = 0; j < map.maxC(); j++)
                if (map.getImage(i,j).isEmpty())
                    return false;
        return true;
    }

    public static void printColumnMapping(List<Set<Integer>> columnMapping) {
        String s = "";
        for (int k = 0; k < columnMapping.size(); k ++) {
            Set<Integer> imgSet = columnMapping.get(k);
            String eString = "";
            eString = k + " -> ";
            String img = "[";
            boolean isFirst = true;
            for (int i : imgSet) {
                if (isFirst) {
                    img += i;
                    isFirst = false;
                } else {
                    img += ", " + i;
                }
            }
            img += "]";
            eString += img;
            s += eString + "\n";
        }
        System.out.println(s);
    }

}