package forward_enumeration.table_enumerator;

import forward_enumeration.enumerative_search.components.EnumAggrTableNode;
import forward_enumeration.context.EnumContext;
import forward_enumeration.context.QueryChest;
import global.GlobalConfig;
import backward_inference.MappingInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.exception.SQLEvalException;
import summarytable.*;
import util.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/28/16.
 * The enumeration algorithm with abstract table representation.
 */
public class SymbolicTableEnumerator extends AbstractTableEnumerator {

    // this is a helper to print SQL queries
    Set<AbstractSummaryTable> countPrinted = new HashSet<>();

    @Override
    public QueryChest enumTable(EnumContext ec, int maxDepth) {

        System.out.println("[FiltersCount format](primitiveSynFilterCount, primitiveBitVecFilterCount, totalBitVecFiltersCount)");

        // this is the list to store all symbolic tables used in enumeration
        List<AbstractSummaryTable> symTables = new ArrayList<>();

        // construct symbolic table for each named table.
        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());

        // build symbolic table for each input table, and store them in SymTables
        List<PrimitiveSummaryTable> symTablesForInputs = qc.getMemoizedTables()
                .stream().map(t -> new PrimitiveSummaryTable(t, new NamedTable(t)))
                .collect(Collectors.toList());
        symTables.addAll(symTablesForInputs);

        System.out.println("[Basic]: " + qc.getMemoizedTables().size() + " [SymTableForInputs]: " + symTablesForInputs.size());

        int aggrNodeCount = 0;
        List<AbstractSummaryTable> symTableForAggr = new ArrayList<>();

        for (PrimitiveSummaryTable st : symTablesForInputs) {

            // core tables to be used in enumerate aggregation
            List<Pair<Table, BVFilter>> instantiated = st.instantiatedAllTablesWithBVFilters(ec);

            for (Pair<Table, BVFilter> p : instantiated) {
                // enumerating aggregation queries,
                // the core of each query is built atop a filtering clause on an input table
                ec.setTableNodes(Arrays.asList(new NamedTable(p.getKey())));
                List<TableNode> ans = EnumAggrTableNode.enumAggregationNodeFlag(ec, EnumAggrTableNode.SIMPLIFY, false);
                for (TableNode an : ans) {
                    try {
                        // build symbolic tables out of aggregation table nodes, and store them for next stage enumeration
                        PrimitiveSummaryTable aggrSt;
                        if (p.getValue().isEmptyFilter()) {
                            aggrSt = new PrimitiveSummaryTable(an.eval(new Environment()), an);
                        } else {
                            aggrSt = new PrimitiveSummaryTable(an.eval(new Environment()), an, new Pair<>(st, p.getValue()));
                        }
                        aggrNodeCount ++;
                        symTableForAggr.add(aggrSt);
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        symTables.addAll(symTableForAggr);

        System.out.println("[Aggregation]: " + aggrNodeCount + " [SymTable]: " + symTables.size());

        List<AbstractSummaryTable> basicAndAggr = new ArrayList<>();
        basicAndAggr.addAll(symTables);

        // only contains symbolic table generated from last stage
        //  (here includes those from aggr and input)

        List<AbstractSummaryTable> stFromLastStage = symTables;

        for (AbstractSummaryTable st : stFromLastStage) {

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
            List<AbstractSummaryTable> collector = new ArrayList<>();
            for (int k = 0; k < stFromLastStage.size(); k ++) {
                for (int l = 0; l < basicAndAggr.size(); l ++) {

                    AbstractSummaryTable st1 = stFromLastStage.get(k);
                    AbstractSummaryTable st2 = basicAndAggr.get(l);

                    // only allow joining between an input table and a compound table
                    if (! ec.getInputs().contains(st2.getBaseTable()))
                        continue;

                    if (st2 instanceof CompoundSummaryTable)
                        continue;

                    CompoundSummaryTable sct = new CompoundSummaryTable(st1, st2);
                    collector.add(sct);
                }
            }

            symTables.addAll(collector);
            stFromLastStage = collector;

            System.out.println("[EnumJoin] level " + i + " [SymTable]: " + symTables.size());

            for (AbstractSummaryTable st : symTables) {

                //System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));

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

            if (qc.getAllCandidates().size() > 0)
                break;
        }

        // Try enumerate by joining two tables from aggregation
        if (qc.getAllCandidates().size() == 0) {
            stFromLastStage = basicAndAggr;

            for (int i = 1; i <= maxDepth; i ++) {
                // used to collect queries generated in the most recent stage
                List<AbstractSummaryTable> collector = new ArrayList<>();
                for (int k = 0; k < stFromLastStage.size(); k ++) {
                    for (int l = 0; l < basicAndAggr.size(); l ++) {

                        AbstractSummaryTable st1 = stFromLastStage.get(k);
                        AbstractSummaryTable st2 = basicAndAggr.get(l);

                        // Does not allow joining between an input table and a compound table
                        if (st2 instanceof CompoundSummaryTable)
                            continue;

                        CompoundSummaryTable sct = new CompoundSummaryTable(st1, st2);
                        collector.add(sct);
                    }
                }

                symTables.addAll(collector);
                stFromLastStage = collector;

                System.out.println("[EnumJoinOnAggr] level " + i + " [SymTable]: " + symTables.size());

                for (AbstractSummaryTable st : stFromLastStage) {
                    //System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));
                    tryEvalToOutput(st, ec, qc);
                }

                if (qc.getAllCandidates().size() > 0)
                    break;
            }
        }

        // Enumerate aggregation on joined tables
        if (qc.getAllCandidates().size() == 0) {

            List<AbstractSummaryTable> basicSymTables = ec.getInputs()
                    .stream().map(t -> new PrimitiveSummaryTable(t, new NamedTable(t)))
                    .collect(Collectors.toList());

            for (int i = 0; i < basicSymTables.size(); i ++) {
                for (int j = i; j < basicSymTables.size(); j ++) {
                    AbstractSummaryTable st1 = basicSymTables.get(i);
                    AbstractSummaryTable st2 = basicSymTables.get(j);

                    CompoundSummaryTable sct = new CompoundSummaryTable(st1, st2);

                    List<Pair<Table, BVFilter>> tablesWFilters = sct.instantiatedAllTablesWithBVFilters(ec);
                    for (Pair<Table, BVFilter> p : tablesWFilters) {
                        // core tables to be used in enumerate aggregation
                        List<TableNode> joinedNodes = new ArrayList<>();
                        joinedNodes.add(new NamedTable(p.getKey()));

                        // enumerating aggregation tables, the aggregation nodes are based on these primitive filters
                        ec.setTableNodes(joinedNodes);
                        List<TableNode> aggregationOnJoinInputs = EnumAggrTableNode.enumAggregationNodeFlag(ec, EnumAggrTableNode.SIMPLIFY, false);

                        List<PrimitiveSummaryTable> enumHaving = new ArrayList<>();

                        for (TableNode an : aggregationOnJoinInputs) {
                            try {
                                enumHaving.add(new PrimitiveSummaryTable(an.eval(new Environment()), an, new Pair<>(sct, p.getValue())));
                            } catch (SQLEvalException e) {
                                e.printStackTrace();
                            }
                        }

                        for (AbstractSummaryTable st : enumHaving) {
                            //System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));
                            tryEvalToOutput(st, ec, qc);
                        }
                    }
                }
            }
        }

        System.out.println("ASymTable Enumeration done: " + (symTables.size()));

        handleEnumerationResult(qc, ec);
        return qc;
    }

    public void handleEnumerationResult(QueryChest qc, EnumContext ec) {

        System.out.println("Candidates: ");

        // Extract the filters to be decoded for each AbstractSymbolicTable from qc
        Map<AbstractSummaryTable, Set<BVFilter>> filtersToDecode = new HashMap<>();
        for (Pair<AbstractSummaryTable, BVFilter> c : qc.getAllCandidates()) {
            if (! filtersToDecode.containsKey(c.getKey())) {
                filtersToDecode.put(c.getKey(), new HashSet<>());
            }
            filtersToDecode.get(c.getKey()).add(c.getValue());
        }

        // generate candidate symFilterTrees for each candidate
        List<BVFilterCompTree> candidateTrees = new ArrayList<>();
        for (Map.Entry<AbstractSummaryTable, Set<BVFilter>> c : filtersToDecode.entrySet()) {
            Map<BVFilter, List<BVFilterCompTree>> cQuery = c.getKey().batchGenDecomposition(c.getValue());
            for (Map.Entry<BVFilter, List<BVFilterCompTree>> i : cQuery.entrySet()) {
                candidateTrees.addAll(i.getValue());
            }
        }

        List<TableNode> unrankedResult = new ArrayList<>();
        System.out.println(candidateTrees.size());

        for (BVFilterCompTree t : candidateTrees) {
            List<TableNode> temp = t.translateToTopSQL(ec);
            unrankedResult.addAll(temp);
        }

        unrankedResult.sort((x,y) -> (x.estimateAllFilterCost() <= y.estimateAllFilterCost() ?
                (x.estimateAllFilterCost() < y.estimateAllFilterCost() ? -1 : 0) : 1));

        int count = 0;
        for (TableNode tn : unrankedResult) {
            if( count >= 20) break;
            try {
                Table t = tn.eval(new Environment());
                List<Integer> projectionIndexes = Table.inferProjection(t, ec.getOutputTable());

                // with projection result printed
                SelectNode stn;

                if ((tn instanceof SelectNode)) {
                    stn = new SelectNode(
                            projectionIndexes.stream().map(x -> ((SelectNode) tn).getColumns().get(x)).collect(Collectors.toList()),
                            ((SelectNode) tn).getTableNode(),
                            ((SelectNode) tn).getFilter());
                } else {
                    stn = new SelectNode(
                            projectionIndexes.stream().map(x -> new NamedVal(tn.getSchema().get(x))).collect(Collectors.toList()),
                            tn,
                            new EmptyFilter());
                }

                Table st = stn.eval(new Environment());
                System.out.println("[No." + (count + 1) + "]===============================");
                count ++;
                System.out.println(stn.prettyPrint(0));
                System.out.println(st);

            } catch (SQLEvalException e) {
                e.printStackTrace();
            }

        }
    }

    // Try to evaluate whether the output table can be derived from symbolic table st.
    private void tryEvalToOutput(AbstractSummaryTable st, EnumContext ec, QueryChest qc) {

        BiConsumer<AbstractSummaryTable, BVFilter> f = (symTable, symFilter) -> {

            if (symFilter.getFilterRep().isEmpty())
                return;

            Table t = symTable.getBaseTable().duplicate();
            t.getContent().clear();
            for (Integer i :symFilter.getFilterRep()) {
                t.getContent().add(symTable.getBaseTable().getContent().get(i).duplicate());
            }

            // add the target to qc if the evaluation result can be projection to be output table
            //    1) size equal
            //    2) can generate a trace
            if( t.getContent().size() == ec.getOutputTable().getContent().size()
                    && !MappingInference.buildMapping(t, ec.getOutputTable()).genColumnMappingInstances().isEmpty())
                qc.insertCandidate(new Pair<>(symTable, symFilter));
        };

        MappingInference mi = MappingInference.buildMapping(st.getBaseTable(), ec.getOutputTable());
        if (! mi.everyCellHasImage())
            return;

        if (GlobalConfig.SPECIAL_TREAT_LAST_STAGE) {
            st.emitFinalVisitAllTables(mi, ec, f);
        } else {
            st.emitInstantiateAllTables(ec, f);
        }
    }

}
