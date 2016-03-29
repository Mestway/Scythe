package enumerator.tableenumerator;

import enumerator.EnumAggrTableNode;
import enumerator.EnumJoinTableNodes;
import enumerator.EnumProjection;
import enumerator.FilterEnumerator;
import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.JoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;
import symbolic.AbstractSymbolicTable;
import symbolic.SymbolicCompoundTable;
import symbolic.SymbolicFilter;
import symbolic.SymbolicTable;
import util.RenameTNWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/28/16.
 */
public class EnumeratingWithAbstractTables extends AbstractTableEnumerator {
    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {
        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());

        List<SymbolicTable> symTables = qc.getRepresentativeTableNodes()
                .stream().filter(tn -> tn instanceof NamedTable)
                .map(t -> SymbolicTable.buildSymbolicTable(((NamedTable) t).getTable(), ec))
                .collect(Collectors.toList());

        List<Table> concreteTables = new ArrayList<>();
        for (SymbolicTable st : symTables) {
            List<Table> tables = st.instantiateAllTables();
            for (Table t : tables) {
                concreteTables.addAll(tables);
            }
        }

        qc.updateQueries(concreteTables.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        System.out.println("FilterNamed: " + concreteTables.size() + " ~ " + qc.getMemoizedTables().keySet().size());


        // enumerating aggregation tables
        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumAggrTableNode.enumAggregationNode(ec)
                .stream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
        qc.updateQueries(tns);

        System.out.println("Aggregation: " + tns.size() + " ~ " + qc.getMemoizedTables().keySet().size());

        List<AbstractSymbolicTable> aSymTables = qc.getRepresentativeTableNodes()
                .stream().filter(tn -> tn instanceof NamedTable)
                .map(t -> SymbolicTable.buildSymbolicTable(((NamedTable) t).getTable(), ec))
                .collect(Collectors.toList());

        List<AbstractSymbolicTable> collector = new ArrayList<>();

        for (int i = 1; i <= depth - 1; i ++) {

            System.out.println(i);

            for (int k = 0; k < aSymTables.size(); k ++) {
                for (int l = k + 1; l < aSymTables.size(); l ++) {
                    AbstractSymbolicTable st1 = aSymTables.get(k);
                    AbstractSymbolicTable st2 = aSymTables.get(l);

                    JoinNode jn = new JoinNode(
                            Arrays.asList(new NamedTable(st1.getBaseTable()),
                                    new NamedTable(st2.getBaseTable())));

                    Table jt = null;

                    try {
                        jt = jn.eval(new Environment());
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }

                    List<Filter> filters = FilterEnumerator.enumAtomicFiltersForJoinedTable(jn,
                            st1.getBaseTable().getSchemaType().size(),
                            st2.getBaseTable().getSchemaType().size(),
                            ec);

                    Set<SymbolicFilter> sfs = new HashSet<>();
                    for (Filter f : filters) {
                        sfs.add(SymbolicFilter.genSymbolicFilter(jt, f));
                    }

                    collector.add(new SymbolicCompoundTable(st1, st2, sfs));
                }
            }
        }

        System.out.println("ASymTable Enumeration done: " + (collector.size() + aSymTables.size()));

        concreteTables = new ArrayList<>();
        for (AbstractSymbolicTable st : collector) {
            List<Table> tables = st.instantiateAllTables();
            for (Table t : tables) {
                concreteTables.addAll(tables);
            }
        }
        for (AbstractSymbolicTable st : aSymTables) {
            List<Table> tables = st.instantiateAllTables();
            for (Table t : tables) {
                concreteTables.addAll(tables);
            }
        }

        System.out.println("Concrete table size: " + concreteTables.size());

        qc.updateQueries(concreteTables.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));

        System.out.println("QCTable Size: " + qc.getRepresentativeTableNodes().size());

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        tns = EnumProjection.enumProjection(ec, ec.getOutputTable());
        qc.updateQueries(tns);

        return qc;
    }
}
