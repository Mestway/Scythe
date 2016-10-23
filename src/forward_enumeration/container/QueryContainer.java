package forward_enumeration.container;

import forward_enumeration.canonical_enum.datastructure.TableLinks;
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
 * The container for storing queries
 */
public class QueryContainer {

    public enum ContainerType { SummaryTableWBV, TableLinks, None }

    ContainerType containerType = ContainerType.None;
    public ContainerType getContainerType() { return this.containerType; }

    // store the getRepresentative table of tables with the same content, to ensure that hash lookup will not mess it up
    private Map<Table, Table> mirror = new HashMap<>();

    public Set<Table> getMemoizedTables() { return this.mirror.keySet(); }

    private QueryContainer() {}

    public QueryContainer(ContainerType containerType) {
        this.containerType = containerType;
    }

    public static QueryContainer initWithInputTables(List<Table> input, ContainerType containerType) {
        QueryContainer qc = new QueryContainer();
        qc.containerType = containerType;
        qc.insertQueries(input.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        return qc;
    }

    public void insertQuery(TableNode tn) {
        try {
            Table t = tn.eval(new Environment());

            if (t.getContent().size() == 0)
                return;

            if (! mirror.containsKey(t))
                mirror.put(t, t);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // insert more queries into the QueryChest
    // (these new tables will be used later)
    public void insertQueries(List<TableNode> queries) {
        for (TableNode tn : queries) {
            insertQuery(tn);
        }
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
    public TableLinks getTableLinks() { return this.tableLinks; }

    // the following two are data structure and functions used in SymbolicTableEnumerator storing candidate summary tables with their corresponding BV filters
    // this set store all candidate constructs, applying projection on candidates
    private Set<Pair<AbstractSummaryTable, BVFilter>> candidates = new HashSet<>();
    public void insertCandidate(Pair<AbstractSummaryTable, BVFilter> p) {
        this.candidates.add(p);
    }
    public Set<Pair<AbstractSummaryTable, BVFilter>> getAllCandidates() {
        return this.candidates;
    }

}
