package forward_enumeration.context;

import forward_enumeration.enumerative_search.datastructure.TableLinks;
import summarytable.AbstractSummaryTable;
import summarytable.BVFilter;
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

    // tabled that is memoized
    private Set<Table> memory = new HashSet<>();

    // store the getRepresentative table of tables with the same content, to ensure that hash lookup will not mess it up
    private Map<Table, Table> mirror = new HashMap<>();
    public Set<Table> getMemoizedTables() { return this.memory; }

    private QueryChest() {}

    public static QueryChest initWithInputTables(List<Table> input) {
        QueryChest qc = new QueryChest();
        qc.insertQueries(input.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        return qc;
    }

    public void insertQuery(TableNode tn) {
        this.insertQueries(Arrays.asList(tn));
    }

    // insert more queries into the QueryChest
    // (these new tables will be used later)
    public void insertQueries(List<TableNode> queries) {
        for (TableNode tn : queries) {
            try {
                Table t = tn.eval(new Environment());

                if (t.getContent().size() == 0)
                    continue;

                if (! memory.contains(t)) {
                    memory.add(t);
                    mirror.put(t, t);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean tableVisited(Table t) {
        return memory.contains(t);
    }

    public List<TableNode> getRepresentativeTableNodes() {
        return this.mirror.entrySet().stream().map(p -> new NamedTable(p.getValue())).collect(Collectors.toList());
    }

    public Table getRepresentative(Table t) {
        return mirror.get(t);
    }

    // the data structure to store what are the ways to generate one table from other tables.
    // this data structure is updated during each enumeration module
    private TableLinks tableLinks = new TableLinks();
    private boolean useTableLinks = false;
    public boolean useTableLinks() { return this.useTableLinks; }
    public void setUseTableLinks() { this.useTableLinks = true; }
    public TableLinks getTableLinks() { return this.tableLinks; }


    // the following two are data structure and functions used in SymbolicTableEnumerator storing candidate summary tables with their corresponding BV filters
    public void insertCandidate(Pair<AbstractSummaryTable, BVFilter> p) {
        this.candidates.add(p);
    }
    public Set<Pair<AbstractSummaryTable, BVFilter>> getAllCandidates() {
        return this.candidates;
    }

}
