package backward_inference;

import javafx.scene.control.Cell;
import lang.sql.ast.Environment;
import lang.sql.ast.predicate.Predicate;
import lang.sql.dataval.NullVal;
import lang.sql.dataval.Value;
import lang.sql.exception.SQLEvalException;
import lang.table.Table;
import lang.table.TableRow;
import util.CombinationGenerator;
import util.DebugHelper;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class used to inference possible mappings from input to output table.
 * Created by clwang on 2/17/16.
 */
public class MappingInferenceExp {

    // program convention in this class: i loops over rows and j loops over columns.

    int maxR, maxC;
    // the mapping between variables
    CellToExpSetMap map;

    private MappingInferenceExp() {}

    public static MappingInferenceExp buildMapping(Table in, Table out) {

        MappingInferenceExp mi = new MappingInferenceExp();

        if (in.getContent().size() == 0 || out.getContent().size() == 0) {
            System.err.println("[Error@MappingInference22]In/Out table is empty.");
        }

        mi.maxR = out.getContent().size();
        mi.maxC = out.getContent().get(0).getValues().size();

        int maxExpSize = 1;
        boolean mappingExist = false;
        while (!mappingExist && maxExpSize <= 2) {
            mi.map = new CellToExpSetMap(mi.maxR, mi.maxC);

            Map<Value, Set<CellIndexExp>> inverseInputTable = inverseTableToExp(in, maxExpSize);

            for (int i = 0; i < out.getContent().size(); i ++) {
                for (int j = 0; j < out.getContent().get(i).getValues().size(); j ++) {
                    Value v = out.getContent().get(i).getValue(j);
                    if (inverseInputTable.containsKey(v)) {
                        for (CellIndexExp ci : inverseInputTable.get(v)) {
                            mi.map.addPair(new CellIndex(i,j), ci);
                        }
                    }
                }
            }

            mi.refineMapping();

            if (mi.everyCellHasImage())
                return mi;

            maxExpSize ++;
        }

        return mi;
    }

    /**
     *  gGenerate a map that maps values to cell indexes expressions
     *  values of these expressions have the same evaluation result
     * @param t the table to be destructed
     * @param maxExpSize the maximum expression size
     * @return a mapping containing all such mappings
     */
    private static Map<Value, Set<CellIndexExp>> inverseTableToExp(Table t, int maxExpSize) {
        Map<Value, Set<CellIndexExp>> inverseTableMap = new HashMap<>();

        if (t.getContent().size() == 0) return inverseTableMap;

        List<Integer> columnIndexList = new ArrayList<>();
        for (int j = 0; j < t.getContent().get(0).getValues().size(); j ++) {
            columnIndexList.add(j);
        }

        // be careful here, we need to add all because a mapping typically requires expressions of multiple sizes
        List<List<Integer>> colCombs = new ArrayList<>();
        for (int k = 1; k <= maxExpSize; k ++)
            colCombs.addAll(CombinationGenerator.genMultPermutation(columnIndexList, k));

        Set<Integer> columnsWNullValues = new HashSet<>();
        for (int r = 0; r < t.getContent().size(); r ++) {
            for (int c = 0; c < t.getContent().get(r).getValues().size(); c ++) {
                if (t.getContent().get(r).getValue(c) instanceof NullVal) {
                    columnsWNullValues.add(c);
                }
            }
        }
        for (int i = 0; i < t.getContent().size(); i ++) {
            for (List<Integer> l : colCombs) {
                for (CellIndexExp.Operator op : CellIndexExp.Operator.values()) {
                    if (op.commutative) {
                        boolean isSorted = true;
                        for (int k = 1; k < l.size(); k ++) {
                            if (! (l.get(k-1) <= l.get(k))) {
                                isSorted = false;
                                break;
                            }
                        }
                        if (! isSorted) continue;
                    }

                    int rowId = i;
                    // obtain concrete values as function parameter
                    List<Value> parameters = l.stream().map(j -> t.getContent().get(rowId).getValue(j)).collect(Collectors.toList());
                    List<CellIndex> paramIndices = l.stream().map(j -> new CellIndex(rowId, j)).collect(Collectors.toList());

                    if (! op.typeCheck(parameters))
                        continue;

                    // special case for IF_NULL
                    if (op == CellIndexExp.Operator.IF_NULL
                            && (!columnsWNullValues.contains(l.get(0)) || l.get(0) == l.get(1)))
                        continue;

                    Value newVal = op.apply(parameters);
                    if (! inverseTableMap.containsKey(newVal))
                        inverseTableMap.put(newVal, new HashSet<>());

                    inverseTableMap.get(newVal).add(new CellIndexExp(op, paramIndices));
                }
            }
        }
        return inverseTableMap;
    }

    // think carefully, we can either refine them into an approximate
    // set or instantiate them fully into concrete mappings,
    // how does this affect the result?
    // Refine mapping is an approximate process, as it is simply trying to remove invalid mappings
    // TODO: calculate the complexity of this algorithm
    // TODO: refine mapping is now integrated into building process
    private void refineMapping() {
        boolean stable = false;
        while (!stable) {
            stable = true;
            for (int i = 0; i < map.maxR(); i ++) {
                for (int j = 0; j < map.maxC(); j ++) {
                    CellIndex src = new CellIndex(i, j);

                    List<CellIndexExp> toRemove = new ArrayList<>();

                    for (CellIndexExp coord : map.getImage(src)) {
                        if (!consistencyCheck(src, coord)) {
                            toRemove.add(coord);
                            stable = false;
                        }
                    }
                    for (CellIndexExp rm : toRemove) {
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
    private boolean consistencyCheck(CellIndex src, CellIndexExp dst) {
        int r = src.r(), c = src.c();
        // corresponding to check 1
        for (int v = 0; v < map.maxC(); v ++) {
            boolean exists = false;
            CellIndex checkingSrc = new CellIndex(r, v);
            for (CellIndexExp indexExp : map.getImage(checkingSrc)) {
                if (CellIndexExp.pairWiseConsistency(new Pair<>(checkingSrc, indexExp), new Pair<>(src, dst)))
                    exists = true;
            }
            if (exists == false)
                return false;
        }
        // corresponding to check 2
        for (int u = 0; u < map.maxR(); u ++) {
            boolean exists = false;
            CellIndex checkingSrc = new CellIndex(u, c);
            for (CellIndexExp indexExp : map.getImage(checkingSrc)) {
                if (CellIndexExp.pairWiseConsistency(new Pair<>(checkingSrc, indexExp), new Pair<>(src, dst)))
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
    // (each element in the mapping is a pair consisting of operator and operands column index)
    public List<Set<Pair<CellIndexExp.Operator, List<Integer>>>> genColumnMappingInstances() {
        List<Set<Pair<CellIndexExp.Operator, List<Integer>>>> columnMapping = new ArrayList<>();
        for (int c = 0; c < maxC; c ++) {
            columnMapping.add(map.getImage(0, c)
                    .stream()
                    .map(x -> x.getColExp())
                    .collect(Collectors.toSet()));
        }
        return columnMapping;
    }

    // it represents how columns in the output table maps to columns in the input table.
    // A list represent a way of mapping, example: l = result[0] represent the first way
    // to map columns in output to columns in input, and l[i] = k means that column i in output
    // maps to column k in the input table.
    // TODO: may be wrong
    public List<List<Pair<CellIndexExp.Operator, List<Integer>>>> genConcreteColMappings() {
        List<Set<Pair<CellIndexExp.Operator, List<Integer>>>> columnMapping = this.genColumnMappingInstances();
        List<List<Pair<CellIndexExp.Operator, List<Integer>>>> listRepColMapping = new ArrayList<>();
        for (int i = 0; i < columnMapping.size(); i ++) {
            listRepColMapping.add(columnMapping.get(i).stream().collect(Collectors.toList()));
        }
        return CombinationGenerator.rotateList(listRepColMapping);
    }

    // We can have some interesting property here:
    //      The row inference result can be obtained even if we only apply the inference on the first column
    // the result is a list of set:
    //      result.get(r) represents the image set of r in the mapping
    public List<Set<Integer>> genRowMappingInstances() {
        List<Set<Integer>> rowMapping = new ArrayList<>();
        for (int r = 0; r < maxR; r ++) {
            rowMapping.add(map.getImage(r, 0).stream().map(x -> x.getRowIndex()).collect(Collectors.toSet()));
        }
        return rowMapping;
    }

    // a mapping instance will map each coordinate in output table to a coordinate in input table
    // the mapping instance is generated through the refined map
    // in the result table, each list in quick coord table should have length 1.
    public List<CellToExpMap> genMappingInstances() {
        CellToExpMap instance = new CellToExpMap(maxR, maxC);
        List<CellToExpMap> resultCollector = new ArrayList<>();
        dfsMappingSearch(0, 0, maxR, maxC, instance, resultCollector, this.map);

        //System.out.println("[MappingInfernce] mapping size " + resultCollector.size());
        return resultCollector;
    }

    /**
     * Perform a dfs search in the mapping to obtain all consistent mapping instances
     * @param i The current row number being searched
     * @param j The current col number being searched
     * @param maxI The bound for i
     * @param maxJ the bound for j
     * @param maxJ the bound for j
     * @param currentMap the partial map for all previous search
     * @param resultCollector a list used to collect search result
     * @param candidatePool the map that is used for search
     */
    private void dfsMappingSearch(
            int i, int j,
            int maxI, int maxJ,
            CellToExpMap currentMap,
            List<CellToExpMap> resultCollector,
            CellToExpSetMap candidatePool) {

        // when dfs reaches its goal
        if (i >= maxI || j >= maxJ) {
            resultCollector.add(currentMap);
            return;
        }

        for (CellIndexExp coord : candidatePool.getImage(i,j)) {
            if (currentMap.consistencyCheck(new CellIndex(i,j), coord)) {
                CellToExpMap nextMap = currentMap.copy();
                nextMap.insert(new CellIndex(i,j), coord);
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


    // a mapping instance will map each coordinate in output table to a coordinate in input table
    // the mapping instance is generated through the refined map
    // in the result table, each list in quick coord table should have length 1.
    public Optional<CellToExpMap> searchOneMappingInstance() {
        CellToExpMap instance = new CellToExpMap(maxR, maxC);
        Optional<CellToExpMap> ctcMap = Optional.ofNullable(
                dfsOneMappingInstance(0, 0, maxR, maxC, instance, this.map));
        return ctcMap;
    }

    // very similar to the one before, but this only search for one mapping instance
    private CellToExpMap dfsOneMappingInstance(
            int i, int j,
            int maxI, int maxJ,
            CellToExpMap currentMap,
            CellToExpSetMap candidatePool) {

        // when dfs reaches its goal
        if (i >= maxI || j >= maxJ) {
            return currentMap;
        }

        for (CellIndexExp coord : candidatePool.getImage(i,j)) {
            if (currentMap.consistencyCheck(new CellIndex(i,j), coord)) {
                CellToExpMap nextMap = currentMap.copy();
                nextMap.insert(new CellIndex(i,j), coord);
                int nextI = i, nextJ = j + 1;
                if (nextJ >= maxJ) {
                    nextJ = nextJ % maxJ;
                    nextI ++;
                }
                CellToExpMap ctcMap = dfsOneMappingInstance(
                        nextI, nextJ,
                        maxI, maxJ,
                        nextMap, candidatePool);

                if (ctcMap != null)
                    return ctcMap;
            }
        }

        return null;
    }

    /**
     * A variant of genMapping Instances,
     * requiring mapping images appear in each range.
     * @param columnRightBoundaries
     *          the set of boundries to test,
     *          e.g. [1 3 5] is asking to find mappings such that [0-1) [1-3) [3-5) have columns inside
     * @return
     */
    public List<CellToExpMap> genMappingInstancesWColumnBarrier(List<Integer> columnRightBoundaries) {

        List<Set<Pair<CellIndexExp.Operator, List<Integer>>>> columnMapping = this.genColumnMappingInstances();
        List<List<Pair<CellIndexExp.Operator, List<Integer>>>> listRepColMapping = new ArrayList<>();
        for (int i = 0; i < columnMapping.size(); i ++) {
            listRepColMapping.add(columnMapping.get(i).stream().collect(Collectors.toList()));
        }

        List<List<Pair<CellIndexExp.Operator, List<Integer>>>> targetsToSearch = CombinationGenerator.rotateList(listRepColMapping);

        List<CellToExpMap> resultCollector = new ArrayList<>();
        for (List<Pair<CellIndexExp.Operator, List<Integer>>> target : targetsToSearch) {

            List<Boolean> inRange = columnRightBoundaries.stream().map(x -> false).collect(Collectors.toList());

            for (Pair<CellIndexExp.Operator, List<Integer>> t : target) {
                for (int k = 0; k < columnRightBoundaries.size(); k ++) {

                    // check whether the mapping is in range
                    int rightBoundary = columnRightBoundaries.get(k);
                    boolean mappingInRange = t.getValue().stream().map(x -> x < rightBoundary).reduce(true, Boolean::logicalAnd);

                    if (mappingInRange) {
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
            if (! flag) continue;

            CellToExpMap instance = new CellToExpMap(maxR, maxC);
            dfsMappingSearchWithFixedColumns(0, 0, maxR, maxC, instance, resultCollector, this.map, target);
        }
        return resultCollector;
    }

    // in this version
    private void dfsMappingSearchWithFixedColumns(
            int i, int j,
            int maxI, int maxJ,
            CellToExpMap currentMap,
            List<CellToExpMap> resultCollector,
            CellToExpSetMap candidatePool,
            List<Pair<CellIndexExp.Operator, List<Integer>>> columnRestriction) {

        // when dfs reaches its goal
        if (i >= maxI || j >= maxJ) {
            resultCollector.add(currentMap);
            return;
        }

        for (CellIndexExp indexExp : candidatePool.getImage(i,j)) {

            Pair<CellIndexExp.Operator, List<Integer>> colExp = indexExp.getColExp();

            boolean permitted = colExp.getKey() == columnRestriction.get(j).getKey()
                    && columnRestriction.get(j).getValue().size() == colExp.getValue().size()
                    && IntStream.range(0, indexExp.operands.size())
                    .mapToObj(k -> colExp.getValue().get(k) == columnRestriction.get(j).getValue().get(k))
                    .reduce(true, Boolean::logicalAnd);

            if (! permitted)
                continue;

            if (currentMap.consistencyCheck(new CellIndex(i,j), indexExp)) {
                CellToExpMap nextMap = currentMap.copy();
                nextMap.insert(new CellIndex(i,j), indexExp);
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

    // The result is the mapping about what are the candidates for each column
    // e.g. if result[0] = {1,2,3}, then it means that the
    // row 0 of the output table can either be from 1,2,3 of the original table.
    public List<Set<Integer>> genRowMappingRange(List<Integer> fixedColumn) {
        CellToExpSetMap newMap = new CellToExpSetMap(maxR, maxC);
        for (int i = 0; i < maxR; i ++) {
            for (int j = 0; j < maxC; j ++) {
                for (CellIndexExp ci : map.getImage(i,j)) {
                    if (fixedColumn.containsAll(ci.getColExp().getValue()))
                        newMap.addPair(new CellIndex(i,j), ci);
                }
            }
        }
        MappingInferenceExp tempMi = new MappingInferenceExp();
        tempMi.maxC  = this.maxC;
        tempMi.maxR = this.maxR;
        tempMi.map = newMap;
        tempMi.refineMapping();

        return tempMi.genRowMappingInstances();
    }

    // key: a row number in the output table
    // value: a row number in the input table
    public static Map<Integer, Integer> rowMappingInference(CellToCellMap cim) {
        Map<Integer, Integer> rowMap = new HashMap<>();
        for (int i = 0; i < cim.maxR(); i ++) {
            rowMap.put(i, cim.getImage(i, 0).r());
        }
        return rowMap;
    }

    public static Map<Integer, Integer> columnMappingInference(CellToCellMap cim) {
        Map<Integer, Integer> columnMap = new HashMap<>();
        for (int j = 0; j < cim.maxC(); j ++) {
            columnMap.put(j, cim.getImage(0, j).c());
        }
        return columnMap;
    }

    @Override
    public String toString() {
        String s = "";
        for (CellIndex key : map.keys()) {
            String listString = map.getImage(key).stream()
                    .map(u -> u.toString())
                    .reduce("", (x, y)-> x.toString() + ","+ y.toString()).trim();
            if (! listString.isEmpty())
                listString = listString.substring(1);
            else
                listString = "[]";
            s += key.toString()
                    + " -> "
                    + (listString.equals("") ? "" : listString) + ";\n";
        }
        return s;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MappingInferenceExp) {
            return this.maxC == ((MappingInferenceExp) obj).maxC
                    && this.maxR == ((MappingInferenceExp) obj).maxR
                    && this.map.equals(((MappingInferenceExp) obj).map);
        }
        return false;
    }

    public MappingInferenceExp deepCopy() {
        MappingInferenceExp mi = new MappingInferenceExp();
        mi.maxC = this.maxC;
        mi.maxR = this.maxR;
        mi.map = this.map.copy();
        return mi;
    }

    public boolean everyCellHasImage() {
        for (int i = 0; i < map.maxR(); i ++)
            for (int j = 0; j < map.maxC(); j++)
                if (map.getImage(i,j).isEmpty())
                    return false;
        return true;
    }

    public static void printColumnMapping(List<Set<Pair<CellIndexExp.Operator, List<Integer>>>> columnMapping) {
        String s = "";
        for (int k = 0; k < columnMapping.size(); k ++) {
            Set<Pair<CellIndexExp.Operator, List<Integer>>> imgSet = columnMapping.get(k);
            String eString = "";
            eString = k + " -> ";
            String imgStr = "[";
            boolean isFirst = true;
            for (Pair<CellIndexExp.Operator, List<Integer>> img : imgSet) {
                if (isFirst) {
                    imgStr += img.getKey() + "_" + img.getValue().stream().map(x -> x.toString()).reduce(String::concat);
                    isFirst = false;
                } else {
                    imgStr += ", " + img.getKey() + "_" + img.getValue().stream().map(x -> x.toString()).reduce(String::concat);
                }
            }
            imgStr += "]";
            eString += imgStr;
            s += eString + "\n";
        }
        System.out.println(s);
    }

}