package enumerator.tableenumerator;

import enumerator.primitive.OneStepQueryInference;
import enumerator.primitive.tables.EnumAggrTableNode;
import enumerator.primitive.EnumCanonicalFilters;
import enumerator.primitive.tables.EnumProjection;
import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import global.GlobalConfig;
import mapping.CoordInstMap;
import mapping.Coordinate;
import mapping.MappingInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
import sql.lang.exception.SQLEvalException;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import symbolic.*;
import util.DebugHelper;
import util.Pair;
import util.RenameTNWrapper;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/28/16.
 */
public class SymbolicTableEnumerator extends AbstractTableEnumerator {

    public int totalQueryCount = 0;

    // this is a helper to print SQL queries
    Set<AbstractSymbolicTable> countPrinted = new HashSet<>();

    @Override
    public QueryChest enumTable(EnumContext ec, int maxDepth) {

        System.out.println("[FiltersCount format](primitiveSynFilterCount, primitiveBitVecFilterCount, totalBitVecFiltersCount)");

        // construct symbolic table for each named table.
        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());
        List<AbstractSymbolicTable> symTables = qc.getMemoizedTables().keySet()
                .stream().map(t -> new SymbolicTable(t, new NamedTable(t)))
                .collect(Collectors.toList());

        System.out.println("[Basic]: " + qc.getMemoizedTables().size() + " [SymTable]: " + symTables.size());

        // core tables to be used in enumerate aggregation
        List<TableNode> tns = new ArrayList<>();
        for (AbstractSymbolicTable st : symTables) {
            tns.addAll(st.instantiateAllTables(ec)
                    .stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        }

        // enumerating aggregation tables, the aggregation nodes are based on these primitive filters
        ec.setTableNodes(tns);

        List<TableNode> ans = EnumAggrTableNode.enumAggregationNodeFlag(ec, EnumAggrTableNode.SIMPLIFY, false);

        // tables returned from last stage are all aggregation nodes
        // build symbolic tables out of aggregation table nodes.
        for (TableNode an : ans) {
            // these tables will be considered as normal, the filters of these aggregation tables
            // are considered as normal named tables: they are stored abstractly, and they will only be evaluated afterwards
            try {
                SymbolicTable st = new SymbolicTable(an.eval(new Environment()), an);
                symTables.add(st);
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }
        System.out.println("[Aggregation]: " + ans.size() + " [SymTable]: " + symTables.size());

        List<AbstractSymbolicTable> basicAndAggr = symTables;

        // only contains symbolic table generated from last stage
        // (here includes those from aggr and input)
        List<AbstractSymbolicTable> stFromLastStage = symTables;

        for (AbstractSymbolicTable st : stFromLastStage) {

            tryEvalToOutput(st, ec, qc, false);

            if (st.allfiltersEnumerated) {
                System.out.println("[FiltersCount]["
                        + "r" + st.getBaseTable().getContent().size()
                        + ", c" + st.getBaseTable().getContent().get(0).getValues().size() + "]("
                        + st.primitiveSynFilterCount + ", "
                        + st.primitiveBitVecFilterCount + ", "
                        + st.totalBitVecFiltersCount + ")");
                countPrinted.add(st);
            }
        }

        for (int i = 1; i <= maxDepth; i ++) {

            // used to collect queries generated in the most recent stage
            List<AbstractSymbolicTable> collector = new ArrayList<>();
            for (int k = 0; k < stFromLastStage.size(); k ++) {
                for (int l = 0; l < basicAndAggr.size(); l ++) {

                    AbstractSymbolicTable st1 = stFromLastStage.get(k);
                    AbstractSymbolicTable st2 = basicAndAggr.get(l);

                    // only allow joining between an input table and a compound table
                    if (! ec.getInputs().contains(st2.getBaseTable()))
                       continue;

                    SymbolicCompoundTable sct = new SymbolicCompoundTable(st1, st2);
                    collector.add(sct);
                }
            }

            symTables.addAll(collector);
            stFromLastStage = collector;

            System.out.println("[EnumJoin] level " + i + " [SymTable]: " + symTables.size());

            for (AbstractSymbolicTable st : symTables) {

                tryEvalToOutput(st, ec, qc, true);

                if (! countPrinted.contains(st)  && st.allfiltersEnumerated) {
                    System.out.println("[FiltersCount][" + "r"
                            + st.getBaseTable().getContent().size() + ", c"
                            + st.getBaseTable().getContent().get(0).getValues().size() + "]("
                            + st.primitiveSynFilterCount + ", "
                            + st.primitiveBitVecFilterCount + ", "
                            + st.totalBitVecFiltersCount + ")");
                    countPrinted.add(st);
                }
            }

            if (qc.runnerUpTable > 0)
                break;

        }

        System.out.println("ASymTable Enumeration done: " + (symTables.size()));

        System.out.println("Runner ups: " + qc.runnerUpTable);

        return qc;
    }

    public static boolean bad_usage_flag = false;
    public static int validFilterBitVecCount = 0;

    private boolean tryEvalToOutput(AbstractSymbolicTable st, EnumContext ec, QueryChest qc, boolean isLastStage) {

        bad_usage_flag = false;
        validFilterBitVecCount = 0;

        BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f = (p, fl) -> {

            if (p.getValue().getFilterRep().isEmpty())
                return;

            Table t = p.getKey().getBaseTable().duplicate();
            t.getContent().clear();
            for (Integer i : p.getValue().getFilterRep()) {
                t.getContent().add(p.getKey().getBaseTable().getContent().get(i).duplicate());
            }

            ec.setTableNodes(Arrays.asList(new NamedTable(t)));

            boolean isRunnerUp = EnumProjection.emitEnumProjection(ec, ec.getOutputTable(), qc);
            if (isRunnerUp) {
                validFilterBitVecCount ++;
                printTopQueries(st, p, ec, fl);
                bad_usage_flag = true;
            }
        };

        MappingInference mi = MappingInference.buildMapping(st.getBaseTable(), ec.getOutputTable());
        if (! mi.isValidMapping())
            return false;

        if (isLastStage && GlobalConfig.SPECIAL_TREAT_LAST_STAGE) {
            st.emitVisitAllTables(mi, ec, f);
        } else {
            st.emitInstantiateAllTables(ec, f);
            System.out.println("[Valid BitVec Count (Runner up)]: " + validFilterBitVecCount);
        }

        return bad_usage_flag;
    }

    // After enumerating projection,
    // we can print out the queries that applied on input can generate output
    private void printTopQueries(
            AbstractSymbolicTable st,
            Pair<AbstractSymbolicTable, SymbolicFilter> p,
            EnumContext ec,
            FilterLinks fl) {

        // TODO: currently just print nothing, should modify this if we want to print top ones
        if (totalQueryCount < 0) {
            int idx = totalQueryCount;

            List<TableNode> runnerUps = st.decodeToQuery(p, ec, fl);

            totalQueryCount += runnerUps.size();
            if (totalQueryCount < 5000) {
                for (TableNode tn : runnerUps) {
                    System.out.println("===[Query " + idx + " ]===" );
                    idx ++;
                    System.out.println(tn.prettyPrint(0));
                    try {
                        System.out.println(tn.eval(new Environment()));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                    //qc.insertQueries(OneStepQueryInference.infer(Arrays.asList(RenameTNWrapper.tryRename(tn)), ec.getOutputTable(), ec));
                }
            }
        }
    }
}
