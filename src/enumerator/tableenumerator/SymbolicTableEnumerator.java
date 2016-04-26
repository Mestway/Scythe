package enumerator.tableenumerator;

import enumerator.primitive.OneStepQueryInference;
import enumerator.primitive.tables.EnumAggrTableNode;
import enumerator.primitive.EnumCanonicalFilters;
import enumerator.primitive.tables.EnumProjection;
import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
import sql.lang.exception.SQLEvalException;
import symbolic.*;
import util.RenameTNWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/28/16.
 */
public class SymbolicTableEnumerator extends AbstractTableEnumerator {

    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {
        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());

        // store all named table symbolically
        List<AbstractSymbolicTable> symTables = qc.getMemoizedTables().keySet()
                .stream().map(t -> new SymbolicTable(t))
                .collect(Collectors.toList());

        System.out.println("[Basic]: " + qc.getMemoizedTables().size() + " [SymTable]: " + symTables.size());

        // enumerating aggregation tables, the aggregation nodes are based on only the input tables
        ec.setTableNodes(qc.getRepresentativeTableNodes());

        List<TableNode> ans = EnumAggrTableNode.enumAggregationNodeFlag(ec, EnumAggrTableNode.SIMPLIFY, false);

        // build symbolic tables out of aggregation table nodes.
        for (TableNode an : ans) {
            // these tables will be considered as normal, the filters of these aggreagtion tables
            // are considered as normal named tables: they are stored abstractly, and they will only be evaluated afterwards
            try {
                SymbolicTable st = new SymbolicTable(an.eval(new Environment()));
                st.setIsFromAggregation();
                symTables.add(st);
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }

        System.out.println("[Aggregation]: " + ans.size() + " [SymTable]: " + symTables.size());

        List<AbstractSymbolicTable> collector = new ArrayList<>();

        for (int i = 1; i <= depth; i ++) {
            for (int k = 0; k < symTables.size(); k ++) {
                for (int l = 0; l < symTables.size(); l ++) {
                    AbstractSymbolicTable st1 = symTables.get(k);
                    AbstractSymbolicTable st2 = symTables.get(l);

                    if (!st2.getBaseTable().equals(ec.getInputs().get(0)))
                        continue;

                    SymbolicCompoundTable sct = new SymbolicCompoundTable(st1, st2);
                    collector.add(sct);
                }
            }

            System.out.println("[EnumJoin]level " + i + " [SymTable]: " + symTables.size());
            symTables.addAll(collector);
        }

        System.out.println("ASymTable Enumeration done: " + (symTables.size()));

        for (AbstractSymbolicTable st : symTables) {
            st.emitInstantiateAllTables(ec, (p, fl) -> {

                Table t = p.getKey().getBaseTable().duplicate();
                t.getContent().clear();
                for (Integer i : p.getValue().getFilterRep()) {
                    t.getContent().add(p.getKey().getBaseTable().getContent().get(i).duplicate());
                }

                ec.setTableNodes(Arrays.asList(new NamedTable(t)));
                boolean isRunnerUp = EnumProjection.emitEnumProjection(ec, ec.getOutputTable(), qc);

                if (isRunnerUp) {
                    List<TableNode> runnerUps = st.decodeToQuery(p, ec, fl);
                    for (TableNode tn : runnerUps) {
                        qc.insertQueries(OneStepQueryInference.infer(Arrays.asList(RenameTNWrapper.tryRename(tn)), ec.getOutputTable(), ec));
                    }
                }
            });
        }

        System.out.println("Runnerups: " + qc.runnerUpTable);

        for (TableNode tn : qc.getResultQueires()) {
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
            System.out.println(tn.prettyPrint(0));
            try {
                System.out.println(tn.eval(new Environment()));
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
        }

        /*
        Set<Table> concreteTables = new HashSet<>();
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
            }
        }

        System.out.println("Concrete table size: " + concreteTables.size());
        qc.updateQueries(concreteTables.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        System.out.println("QCTable Size: " + qc.getRepresentativeTableNodes().size());

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> tns = EnumProjection.enumProjection(ec, ec.getOutputTable());
        qc.updateQueries(tns);*/


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
