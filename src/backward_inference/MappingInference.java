package backward_inference;

import sql.lang.datatype.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
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
    CellToCellSetMap map = new CellToCellSetMap();

    private MappingInference() {}

    public static MappingInference buildMapping(Table in, Table out) {

        MappingInference mi = new MappingInference();

        if (in.getContent().size() == 0 || out.getContent().size() == 0) {
            System.err.println("[Error@MappingInference22]In/Out table is empty.");
        }

        mi.maxR = out.getContent().size();
        mi.maxC = out.getContent().get(0).getValues().size();
        mi.map.initialize(mi.maxR, mi.maxC);
        mi.input = in; mi.output = out;

        Map<Value, Set<CellIndex>> inverseInputTable = inverseTable(mi.input);

        for (int i = 0; i < mi.output.getContent().size(); i ++) {
            for (int j = 0; j < mi.output.getContent().get(i).getValues().size(); j ++) {
                Value v = mi.output.getContent().get(i).getValue(j);

                if (inverseInputTable.containsKey(v)) {
                    for (CellIndex ci : inverseInputTable.get(v)) {
                        mi.map.addPair(new CellIndex(i,j), ci);
                    }
                }
            }
        }

        mi.refineMapping();
        return mi;
    }

    // generate a map that maps values to cell indexes, where values of these cells have the same value as the key
    private static Map<Value, Set<CellIndex>> inverseTable(Table t) {
        Map<Value, Set<CellIndex>> inverseTableMap = new HashMap<>();
        for (int i = 0; i < t.getContent().size(); i ++) {
            for (int j = 0; j < t.getContent().get(i).getValues().size(); j ++) {

                Value cellVal = t.getContent().get(i).getValue(j);

                if (! inverseTableMap.containsKey(cellVal))
                    inverseTableMap.put(cellVal, new HashSet<>());

                inverseTableMap.get(cellVal).add(new CellIndex(i, j));
            }
        }
        return inverseTableMap;
    }

    // think carefully, we can either refine them into an approximate
    // set or instantiate them fully into concrete mappings,
    // how does this affect the result?
    // Refine mapping is an approximate process, as it is simply trying to remove invalid mappings
    // TODO: calculate the complexity of this algorithm
    // TODO: refind mapping is now integrated into building process
    private void refineMapping() {
        boolean stable = false;
        while (!stable) {
            stable = true;
            for (int i = 0; i < map.maxR(); i ++) {
                for (int j = 0; j < map.maxC(); j ++) {
                    CellIndex src = new CellIndex(i, j);
                    List<CellIndex> toRemove = new ArrayList<>();
                    for (CellIndex coord : map.getImage(src)) {
                        if (!consistencyCheck(src, coord)) {
                            toRemove.add(coord);
                            stable = false;
                        }
                    }
                    for (CellIndex rm : toRemove) {
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
    private boolean consistencyCheck(CellIndex src, CellIndex dst) {
        int r = src.r(), c = src.c();
        int rPrim = dst.r(), cPrim = dst.c();

        // corresponding to check 1
        for (int v = 0; v < map.maxC(); v ++) {
            boolean exists = false;
            for (CellIndex coord : map.getImage(new CellIndex(r, v))) {
                if (coord.r() == rPrim)
                    exists = true;
            }
            if (exists == false)
                return false;
        }

        // corresponding to check 2
        for (int u = 0; u < map.maxR(); u ++) {
            boolean exists = false;
            for (CellIndex coord : map.getImage(new CellIndex(u, c))) {
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
            columnMapping.add(map.getImage(0, c).stream().map(x -> x.c()).collect(Collectors.toSet()));
        }
        return columnMapping;
    }

    // We can have some interesting property here:
    //      The row inference result can be obtained even if we only apply the inference on the first column
    // the result is a list of set:
    //      result.get(r) represents the image set of r in the mapping
    public List<Set<Integer>> genRowMappingInstances() {
        List<Set<Integer>> rowMapping = new ArrayList<>();
        for (int r = 0; r < maxR; r ++) {
            rowMapping.add(map.getImage(r, 0).stream().map(x -> x.r()).collect(Collectors.toSet()));
        }
        return rowMapping;
    }

    // a mapping instance will map each coordinate in output table to a coordinate in input table
    // the mapping instance is generated through the refined map
    // in the result table, each list in quick coord table should have length 1.
    public List<CellToCellMap> genMappingInstances() {
        CellToCellMap instance = new CellToCellMap();
        instance.initialize(maxR, maxC);
        List<CellToCellMap> resultCollector = new ArrayList<>();
        dfsMappingSearch(0, 0, maxR, maxC, instance, resultCollector, this.map);

        //System.out.println("[MappingInfernce] mapping size " + resultCollector.size());
        return resultCollector;
    }

    private void dfsMappingSearch(
            int i, int j,
            int maxI, int maxJ,
            CellToCellMap currentMap,
            List<CellToCellMap> resultCollector,
            CellToCellSetMap candidatePool) {

        // when dfs reaches its goal
        if (i >= maxI || j >= maxJ) {
            resultCollector.add(currentMap);
            return;
        }

        for (CellIndex coord : candidatePool.getImage(i,j)) {
            if (currentMap.consistencyCheck(new CellIndex(i,j), coord)) {
                CellToCellMap nextMap = currentMap.deepCopy();
                nextMap.addMap(new CellIndex(i,j), coord);
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

    /**
     * A variant of genMapping Instances,
     * requiring mapping images appear in each range.
     * @param columnRightBoundaries
     *          the set of boundries to test,
     *          e.g. [1 3 5] is asking to find mappings such that [0-1) [1-3) [3-5) have columns inside
     * @return
     */
    public List<CellToCellMap> genMappingInstancesWColumnBarrier(List<Integer> columnRightBoundaries) {

        List<Set<Integer>> columnMapping = this.genColumnMappingInstances();
        List<List<Integer>> listRepColMapping = new ArrayList<>();
        for (int i = 0; i < columnMapping.size(); i ++) {
            listRepColMapping.add(columnMapping.get(i).stream().collect(Collectors.toList()));
        }

        List<List<Integer>> targetsToSearch = CombinationGenerator.rotateList(listRepColMapping);

        List<CellToCellMap> resultCollector = new ArrayList<>();
        for (List<Integer> target : targetsToSearch) {

            List<Boolean> inRange = columnRightBoundaries.stream().map(x -> false).collect(Collectors.toList());

            for (Integer t : target) {
                for (int k = 0; k < columnRightBoundaries.size(); k ++) {
                    if (t < columnRightBoundaries.get(k)) {
                        inRange.set(k, true);
                        break;
                    }
                }
            }

            boolean flag = true;
            for (int k = 0; k < inRange.size(); k ++) {
                flag = flag && inRange.get(k);
            }

            // if the size is not 1, we can easily solve it even without push down
            //if (target.size() > 1 && ! flag)
            if (! flag)
                continue;

            CellToCellMap instance = new CellToCellMap();
            instance.initialize(maxR, maxC);
            dfsMappingSearchWithFixedColumns(0, 0, maxR, maxC, instance, resultCollector, this.map, target);
        }

        return resultCollector;
    }

    // in this version
    private void dfsMappingSearchWithFixedColumns(
            int i, int j,
            int maxI, int maxJ,
            CellToCellMap currentMap,
            List<CellToCellMap> resultCollector,
            CellToCellSetMap candidatePool,
            List<Integer> columnRestriction) {

        // when dfs reaches its goal
        if (i >= maxI || j >= maxJ) {
            resultCollector.add(currentMap);
            return;
        }

        for (CellIndex coord : candidatePool.getImage(i,j)) {
            if (coord.c() != columnRestriction.get(j))
                continue;

            if (currentMap.consistencyCheck(new CellIndex(i,j), coord)) {
                CellToCellMap nextMap = currentMap.deepCopy();
                nextMap.addMap(new CellIndex(i,j), coord);
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
    public static Map<Integer, Integer> rowMappingInference(CellToCellMap cim) {
        Map<Integer, Integer> rowMap = new HashMap<>();
        for (int i = 0; i < cim.maxR(); i ++) {
            rowMap.put(i, cim.getMap().get(i).get(0).r());
        }
        return rowMap;
    }

    public static Map<Integer, Integer> columnMappingInference(CellToCellMap cim) {
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
                for (int k = 0; k < table.getSchema().size(); k ++) {
                    rowBinding.put(
                            table.getName() + "." + table.getSchema().get(k),
                            tr.getValue(k));
                }
            } else {
                for (int k = 0; k < table.getSchema().size(); k ++) {
                    rowBinding.put(table.getSchema().get(k), tr.getValue(k));
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
                for (int k = 0; k < table.getSchema().size(); k ++) {
                    rowBinding.put(table.getName() + "." + table.getSchema().get(k), tr.getValue(k));
                }
            } else {
                for (int k = 0; k < table.getSchema().size(); k ++) {
                    rowBinding.put(table.getSchema().get(k), tr.getValue(k));
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
        for (Map.Entry<CellIndex, Set<CellIndex>> t : map.toMap().entrySet()) {
            String listString = t.getValue().stream()
                    .map(u -> u.toString())
                    .reduce("", (x, y)-> x.toString() + ","+ y.toString()).trim();
            s += t.getKey().toString()
                    + " -> " + " ["
                    + (listString.equals("") ? "" : listString.substring(listString.indexOf("("))) + "]\n";
        }
        return s;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MappingInference) {
            return this.input.equals(((MappingInference) obj).input)
                    && this.output.equals(((MappingInference) obj).output)
                    && this.maxC == ((MappingInference) obj).maxC
                    && this.maxR == ((MappingInference) obj).maxR
                    && this.map.equals(((MappingInference) obj).map);
        }
        return false;
    }

    public boolean everyCellHasImage() {
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