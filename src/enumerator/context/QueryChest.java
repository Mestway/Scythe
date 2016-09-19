package enumerator.context;

import symbolic.AbstractSummaryTable;
import symbolic.BVFilter;
import util.Pair;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 * This class is used to store enumerated queries
 */
public class QueryChest {

    // this set store all candidate constructs, applying projection on candidates
    private Set<Pair<AbstractSummaryTable, BVFilter>> candidates = new HashSet<>();

    // these two fields are used for evaluating enumeration
    @Deprecated
    public int queryCount = 0;

    // count the number of table before projection that can be projected into true output
    @Deprecated
    public int runnerUpTable = 0;

    // tabled that is memoized
    private Map<Table, List<TableNode>> memory = new HashMap<>();

    // store the representative table of tables with the same content
    private Map<Table, Table> mirror = new HashMap<>();

    // the data structure to store what are the ways to generate one table from other tables.
    // this data structure is updated during each enumeration module
    private TableEdgeSet edges = new TableEdgeSet();
    private boolean enableExport = false;

    public boolean isEnableExport() { return this.enableExport; }
    public void setEnableExport() { this.enableExport = true; }

    public Map<Table, List<TableNode>> getMemoizedTables() { return this.memory; }
    public TableEdgeSet getEdges() { return this.edges; }

    private QueryChest() {}
    public static QueryChest initWithInputTables(List<Table> input) {
        QueryChest qc = new QueryChest();
        qc.updateQueries(input.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        return qc;
    }

    public void updateQuery(TableNode tn) {
        this.updateQueries(Arrays.asList(tn));
    }

    // update the QueryChest adding new tables
    // (these new tables will be used later)
    public void updateQueries(List<TableNode> queries) {
        queryCount ++;
        for (TableNode tn : queries) {
            try {
                Table t = tn.eval(new Environment());

                if (t.getContent().size() == 0)
                    continue;
                if (memory.containsKey(t)) {
                    //memory.get(t).add(tn);
                } else {
                    ArrayList<TableNode> ar = new ArrayList<>();
                    ar.add(tn);
                    memory.put(t, ar);
                    mirror.put(t, t);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<TableNode> lookup(Table t) {
        if (memory.containsKey(t)) {
            return memory.get(t);
        }
        else return null;
    }

    public List<TableNode> getRepresentativeTableNodes() {
        return this.memory.keySet().stream().map(t -> new NamedTable(t)).collect(Collectors.toList());
    }

    public Table representative(Table t) {
        return mirror.get(t);
    }

    public void insertCandidate(Pair<AbstractSummaryTable, BVFilter> p) {
        this.candidates.add(p);
    }

    public Set<Pair<AbstractSummaryTable, BVFilter>> getAllCandidates() {
        return this.candidates;
    }

}
