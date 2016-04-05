package enumerator.tableenumerator;

import enumerator.primitive.EnumAggrTableNode;
import enumerator.primitive.EnumCanonicalFilters;
import enumerator.primitive.EnumProjection;
import enumerator.primitive.FilterEnumerator;
import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
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
public class SymbolicTableEnumerator extends AbstractTableEnumerator {

    boolean DEBUG = false;

    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {
        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());

        List<AbstractSymbolicTable> symTables = qc.getRepresentativeTableNodes()
                .stream().filter(tn -> tn instanceof NamedTable)
                .map(t -> SymbolicTable.buildSymbolicTable(((NamedTable) t).getTable(), ec))
                .collect(Collectors.toList());

        Set<Table> concreteTables = new HashSet<>();
        for (AbstractSymbolicTable st : symTables) {
            List<Table> tables = st.instantiateAllTables();
            for (Table t : tables) {
                concreteTables.add(t);
            }
        }

        qc.updateQueries(concreteTables.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        System.out.println("FilterNamed: " + concreteTables.size() + " ~ " + qc.getMemoizedTables().keySet().size());

        // enumerating aggregation tables
        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<AggregationNode> ans = EnumAggrTableNode.enumAggregationNode(ec);

        // build symbolic tables out of aggregation table nodes.
        for (AggregationNode an : ans) {
            RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(an);
            List<Filter> anFilters = new ArrayList<>(); //EnumCanonicalFilters.enumCanonicalFilterAggrNode(rt, ec);
            try {
                symTables.add(new SymbolicTable(an.eval(new Environment()),
                        anFilters.stream().map(f -> SymbolicFilter.genSymbolicFilterFromTableNode(rt, f)).collect(Collectors.toSet())));
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Aggregation: " + ans.size() + " ~ " + qc.getMemoizedTables().keySet().size());

        List<AbstractSymbolicTable> collector = new ArrayList<>();

        for (int i = 1; i <= depth; i ++) {

            System.out.println("[Level] " + i);

            for (int k = 0; k < symTables.size(); k ++) {
                for (int l = 0; l < symTables.size(); l ++) {
                    AbstractSymbolicTable st1 = symTables.get(k);
                    AbstractSymbolicTable st2 = symTables.get(l);

                    if (!st2.getBaseTable().equals(ec.getInputs().get(0)))
                        continue;

                    if (DEBUG && st1.getBaseTable().contentStrictEquals(ec.getInputs().get(0)) && st2.getBaseTable().getContent().size() == 2) {
                        System.out.println("||~~~~~~~~~~~~~~~~");
                        System.out.println(st1.getBaseTable());
                        System.out.println(st2.getBaseTable());
                    }

                    JoinNode jn = new JoinNode(
                            Arrays.asList(new NamedTable(st1.getBaseTable()),
                                    new NamedTable(st2.getBaseTable())));
                    RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);



                    Table jt = null;

                    try {
                        jt = rt.eval(new Environment());
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }

                    List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterJoinNode(rt, ec);



                    Set<SymbolicFilter> sfs = new HashSet<>();
                    for (Filter f : filters) {
                        sfs.add(SymbolicFilter.genSymbolicFilter(jt, f));
                    }

                    SymbolicCompoundTable sct =new SymbolicCompoundTable(st1, st2, sfs);

                    if (DEBUG && st1.getBaseTable().contentStrictEquals(ec.getInputs().get(0)) && st2.getBaseTable().getContent().size() == 2) {
                        System.out.println(jt);
                        for (Table t : sct.instantiateAllTables())
                            if (t.getContent().size() == 2) {
                                System.out.println(t);
                            }
                        System.out.println(ec.getOutputTable());
                        System.out.println("||~~~~~~~~~~~~~~~~");
                    }

                    collector.add(sct);
                }
            }
            symTables.addAll(collector);
        }

        System.out.println("ASymTable Enumeration done: " + (symTables.size()));

        concreteTables = new HashSet<>();
        int kk = 0;
        for (AbstractSymbolicTable st : symTables) {
            kk ++;
            System.out.println(kk + " : " + concreteTables.size() + " : " + st.getPrimitiveFilterNum());
            if (kk > 180) {
                System.out.println(((SymbolicCompoundTable) st).st1.getBaseTable());
                System.out.println(((SymbolicCompoundTable) st).st2.getBaseTable());

            }
            List<Table> tables = st.instantiateAllTables();
            for (Table t : tables) {

                concreteTables.add(t);
                if (DEBUG && st instanceof SymbolicCompoundTable) {
                    if (((SymbolicCompoundTable) st).st1.getBaseTable().contentStrictEquals(ec.getInputs().get(0))
                            && ((SymbolicCompoundTable) st).st2.getBaseTable().getContent().size() == 2)
                    System.out.println(t);
                }
            }
        }

        System.out.println("Concrete table size: " + concreteTables.size());
        qc.updateQueries(concreteTables.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        System.out.println("QCTable Size: " + qc.getRepresentativeTableNodes().size());

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumProjection.enumProjection(ec, ec.getOutputTable());
        qc.updateQueries(tns);


        /*
        for (Table t : qc.getMemoizedTables().keySet()) {
            if (t.getContent().size() == 2 && t.getMetadata().size() == 3) {
                System.out.println(t);
            }
        }

        System.out.println(ec.getOutputTable());*/

        return qc;
    }
}
