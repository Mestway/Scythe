package enumerator.tableenumerator;

import enumerator.primitive.tables.EnumAggrTableNode;
import enumerator.primitive.tables.EnumProjection;
import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import global.GlobalConfig;
import mapping.MappingInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.*;
import sql.lang.exception.SQLEvalException;
import symbolic.*;
import util.Pair;

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

        List<AbstractSymbolicTable> basicAndAggr = new ArrayList<>();
        basicAndAggr.addAll(symTables);

        // only contains symbolic table generated from last stage
        //  (here includes those from aggr and input)

        List<AbstractSymbolicTable> stFromLastStage = symTables;

        for (AbstractSymbolicTable st : stFromLastStage) {

            tryEvalToOutput(st, ec, qc);

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

        // Synthesis of JOIN, can be up to 2 levels of nested joins
        for (int i = 1; i <= maxDepth; i ++) {

            // used to collect queries generated in the most recent stage
            List<AbstractSymbolicTable> collector = new ArrayList<>();
            for (int k = 0; k < stFromLastStage.size(); k ++) {
                for (int l = 0; l < basicAndAggr.size(); l ++) {

                    AbstractSymbolicTable st1 = stFromLastStage.get(k);
                    AbstractSymbolicTable st2 = basicAndAggr.get(l);

                    // only allow joining between an input table and a compound table
                    //if (!GlobalConfig.ALLOW_JOIN_NONE_INPUT_TABLE && ! ec.getInputs().contains(st2.getBaseTable()))
                    if (! ec.getInputs().contains(st2.getBaseTable()))
                            continue;

                    if (st2 instanceof SymbolicCompoundTable)
                        continue;

                    SymbolicCompoundTable sct = new SymbolicCompoundTable(st1, st2);
                    collector.add(sct);
                }
            }

            symTables.addAll(collector);
            stFromLastStage = collector;

            System.out.println("[EnumJoin] level " + i + " [SymTable]: " + symTables.size());

            for (AbstractSymbolicTable st : symTables) {

                System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));

                tryEvalToOutput(st, ec, qc);

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

        // Try enumerate by joining two tables from aggregation
        if (qc.runnerUpTable == 0) {
            stFromLastStage = basicAndAggr;

            for (int i = 1; i <= maxDepth; i ++) {
                // used to collect queries generated in the most recent stage
                List<AbstractSymbolicTable> collector = new ArrayList<>();
                for (int k = 0; k < stFromLastStage.size(); k ++) {
                    for (int l = 0; l < basicAndAggr.size(); l ++) {

                        AbstractSymbolicTable st1 = stFromLastStage.get(k);
                        AbstractSymbolicTable st2 = basicAndAggr.get(l);

                        // Does not allow joining between an input table and a compound table
                        if (st2 instanceof SymbolicCompoundTable)
                            continue;

                        SymbolicCompoundTable sct = new SymbolicCompoundTable(st1, st2);
                        collector.add(sct);
                    }
                }

                symTables.addAll(collector);
                stFromLastStage = collector;

                System.out.println("[EnumJoinOnAggr] level " + i + " [SymTable]: " + symTables.size());

                for (AbstractSymbolicTable st : stFromLastStage) {
                    System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));
                    tryEvalToOutput(st, ec, qc);
                }

                if (qc.runnerUpTable > 0)
                    break;

            }
        }

        // Enumerate aggregation on joined tables
        if (qc.runnerUpTable == 0) {

            List<AbstractSymbolicTable> basicSymTables = ec.getInputs()
                    .stream().map(t -> new SymbolicTable(t, new NamedTable(t)))
                    .collect(Collectors.toList());

            for (int i = 0; i < basicSymTables.size(); i ++) {
                for (int j = i; j < basicSymTables.size(); j ++) {
                    AbstractSymbolicTable st1 = basicSymTables.get(i);
                    AbstractSymbolicTable st2 = basicSymTables.get(j);

                    SymbolicCompoundTable sct = new SymbolicCompoundTable(st1, st2);

                    // core tables to be used in enumerate aggregation
                    List<TableNode> joinedNodes = new ArrayList<>();
                    joinedNodes.addAll(sct.instantiateAllTables(ec)
                            .stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));

                    // enumerating aggregation tables, the aggregation nodes are based on these primitive filters
                    ec.setTableNodes(joinedNodes);

                    List<TableNode> aggregationOnJoinInputs = EnumAggrTableNode.enumAggregationNodeFlag(ec, EnumAggrTableNode.SIMPLIFY, false);
                    List<SymbolicTable> enumHaving = new ArrayList<>();

                    for (TableNode an : aggregationOnJoinInputs) {
                        try {
                            enumHaving.add(new SymbolicTable(an.eval(new Environment()), an));
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                    }

                    for (AbstractSymbolicTable st : enumHaving) {
                        System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));
                        tryEvalToOutput(st, ec, qc);
                    }
                }
            }
        }

        System.out.println("ASymTable Enumeration done: " + (symTables.size()));

        System.out.println("Runner ups: " + qc.runnerUpTable);

        System.out.println("Candidates: ");

        for (Pair<AbstractSymbolicTable, SymbolicFilter> c : qc.getAllCandidates()) {

            Set<SymbolicFilter> toDecode = new HashSet<SymbolicFilter>();
            toDecode.add(c.getValue());

            Map<SymbolicFilter, List<SymFilterCompTree>> cQuery = c.getKey().batchGenDecomposition(toDecode);

            for (Map.Entry<SymbolicFilter, List<SymFilterCompTree>> i : cQuery.entrySet()) {
                for (SymFilterCompTree t :i.getValue()) {
                    System.out.println("\t[Tree] " + t.prettyString(2).trim());
                    System.out.println("===============================");
                    System.out.println(t.translateToTopSQL(ec).prettyPrint(0));
                    try {
                        System.out.println(t.translateToTopSQL(ec).eval(new Environment()));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return qc;
    }

    // Try to evaluate whether the output table can be derived from symbolic table st.
    private void tryEvalToOutput(AbstractSymbolicTable st, EnumContext ec, QueryChest qc) {

        BiConsumer<AbstractSymbolicTable, SymbolicFilter> f = (symTable, symFilter) -> {

            if (symFilter.getFilterRep().isEmpty())
                return;

            Table t = symTable.getBaseTable().duplicate();
            t.getContent().clear();
            for (Integer i :symFilter.getFilterRep()) {
                t.getContent().add(symTable.getBaseTable().getContent().get(i).duplicate());
            }

            ec.setTableNodes(Arrays.asList(new NamedTable(t)));

            boolean isRunnerUp = EnumProjection.emitEnumProjection(ec, ec.getOutputTable(), qc);
            if (isRunnerUp) {
                qc.insertCandidate(new Pair<>(symTable, symFilter));
                System.out.println("Find one here!");
                //printTopQueries(st, p, ec, fl);
            }
        };

        MappingInference mi = MappingInference.buildMapping(st.getBaseTable(), ec.getOutputTable());
        if (! mi.isValidMapping())
            return;

        if (GlobalConfig.SPECIAL_TREAT_LAST_STAGE) {
            st.emitFinalVisitAllTables(mi, ec, f);
        } else {
            st.emitInstantiateAllTables(ec, f);
        }
    }

}
