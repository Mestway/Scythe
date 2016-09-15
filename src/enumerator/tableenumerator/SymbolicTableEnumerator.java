package enumerator.tableenumerator;

import enumerator.primitive.tables.EnumAggrTableNode;
import enumerator.primitive.tables.EnumProjection;
import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import global.GlobalConfig;
import mapping.MappingInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.exception.SQLEvalException;
import symbolic.*;
import util.Pair;
import util.TableInstanceParser;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/28/16.
 * The enumeration algorithm with abstract table representation.
 */
public class SymbolicTableEnumerator extends AbstractTableEnumerator {

    // this is a helper to print SQL queries
    Set<AbstractSymbolicTable> countPrinted = new HashSet<>();

    @Override
    public QueryChest enumTable(EnumContext ec, int maxDepth) {

        System.out.println("[FiltersCount format](primitiveSynFilterCount, primitiveBitVecFilterCount, totalBitVecFiltersCount)");

        // this is the list to store all symbolic tables used in enumeration
        List<AbstractSymbolicTable> symTables = new ArrayList<>();

        // construct symbolic table for each named table.
        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());

        // build symbolic table for each input table, and store them in SymTables
        List<SymbolicTable> symTablesForInputs = qc.getMemoizedTables().keySet()
                .stream().map(t -> new SymbolicTable(t, new NamedTable(t)))
                .collect(Collectors.toList());
        symTables.addAll(symTablesForInputs);

        System.out.println("[Basic]: " + qc.getMemoizedTables().size() + " [SymTableForInputs]: " + symTablesForInputs.size());

        int aggrNodeCount = 0;
        List<AbstractSymbolicTable> symTableForAggr = new ArrayList<>();

        for (SymbolicTable st : symTablesForInputs) {
            // core tables to be used in enumerate aggregation
            List<Pair<Table, SymbolicFilter>> instantiated = st.instantiatedAllTablesWithSymFilters(ec);

            for (Pair<Table, SymbolicFilter> p : instantiated) {
                // enumerating aggregation queries,
                // the core of each query is built atop a filtering clause on an input table
                ec.setTableNodes(Arrays.asList(new NamedTable(p.getKey())));
                List<TableNode> ans = EnumAggrTableNode.enumAggregationNodeFlag(ec, EnumAggrTableNode.SIMPLIFY, false);
                for (TableNode an : ans) {
                    try {
                        // build symbolic tables out of aggregation table nodes, and store them for next stage enumeration
                        SymbolicTable aggrSt;
                        if (p.getValue().isEmptyFilter()) {
                            aggrSt = new SymbolicTable(an.eval(new Environment()), an);
                        } else {
                            aggrSt = new SymbolicTable(an.eval(new Environment()), an, new Pair<>(st, p.getValue()));
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

                //System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));

                Table t = TableInstanceParser.parseMarkDownTable("temp",
                "| [T3].Name | [T3].MAX-QTY | input1.Name | input1.Price | input1.QTY | input1.CODE | \r\n" +
                        "| ------------------------------------------ | \r\n" +
                        "| Bottle | 41.0 | Rope | 3.6 | 35.0 | 236.0  |\r\n" +
                        "| Bottle | 41.0 | Chain | 2.8 | 15.0 | 237.0  |\r\n" +
                        "| Bottle | 41.0 | Paper | 1.6 | 45.0 | 124.0  |\r\n" +
                        "| Bottle | 41.0 | Bottle | 4.5 | 41.0 | 478.0  |\r\n" +
                        "| Bottle | 41.0 | Bottle | 1.8 | 12.0 | 123.0  |\r\n" +
                        "| Bottle | 41.0 | Computer | 1450.75 | 71.0 |  784.0 |\r\n" +
                        "| Bottle | 41.0 | Spoon | 0.7 | 10.0 | 412.0  |\r\n" +
                        "| Bottle | 41.0 | Bottle | 1.3 | 15.0 | 781.0  |\r\n" +
                        "| Bottle | 41.0 | Rope | 0.9 | 14.0 | 965.0  |\r\n");


                if (st.getBaseTable().contentEquals(t))
                    System.out.println("aha..");

                tryEvalToOutput(st, ec, qc);

                System.out.println(st.getBaseTable());

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
                    //System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));
                    tryEvalToOutput(st, ec, qc);
                }

                if (qc.getAllCandidates().size() > 0)
                    break;
            }
        }

        // Enumerate aggregation on joined tables
        if (qc.getAllCandidates().size() == 0) {

            List<AbstractSymbolicTable> basicSymTables = ec.getInputs()
                    .stream().map(t -> new SymbolicTable(t, new NamedTable(t)))
                    .collect(Collectors.toList());

            for (int i = 0; i < basicSymTables.size(); i ++) {
                for (int j = i; j < basicSymTables.size(); j ++) {
                    AbstractSymbolicTable st1 = basicSymTables.get(i);
                    AbstractSymbolicTable st2 = basicSymTables.get(j);

                    SymbolicCompoundTable sct = new SymbolicCompoundTable(st1, st2);

                    List<Pair<Table, SymbolicFilter>> tablesWFilters = sct.instantiatedAllTablesWithSymFilters(ec);
                    for (Pair<Table, SymbolicFilter> p : tablesWFilters) {
                        // core tables to be used in enumerate aggregation
                        List<TableNode> joinedNodes = new ArrayList<>();
                        joinedNodes.add(new NamedTable(p.getKey()));

                        // enumerating aggregation tables, the aggregation nodes are based on these primitive filters
                        ec.setTableNodes(joinedNodes);
                        List<TableNode> aggregationOnJoinInputs = EnumAggrTableNode.enumAggregationNodeFlag(ec, EnumAggrTableNode.SIMPLIFY, false);

                        List<SymbolicTable> enumHaving = new ArrayList<>();

                        for (TableNode an : aggregationOnJoinInputs) {
                            try {
                                enumHaving.add(new SymbolicTable(an.eval(new Environment()), an, new Pair<>(sct, p.getValue())));
                            } catch (SQLEvalException e) {
                                e.printStackTrace();
                            }
                        }

                        for (AbstractSymbolicTable st : enumHaving) {
                            //System.out.println("\t" + st.getBaseTable().getMetadata().stream().reduce("", (x,y)-> x+ ", " + y).substring(1));
                            tryEvalToOutput(st, ec, qc);
                        }
                    }
                }
            }
        }

        System.out.println("ASymTable Enumeration done: " + (symTables.size()));
        System.out.println("Runner ups: " + qc.runnerUpTable);

        handleEnumerationResult(qc, ec);
        return qc;
    }

    public void handleEnumerationResult(QueryChest qc, EnumContext ec) {

        System.out.println("Candidates: ");

        // Extract the filters to be decoded for each AbstractSymbolicTable from qc
        Map<AbstractSymbolicTable, Set<SymbolicFilter>> filtersToDecode = new HashMap<>();
        for (Pair<AbstractSymbolicTable, SymbolicFilter> c : qc.getAllCandidates()) {
            if (! filtersToDecode.containsKey(c.getKey())) {
                filtersToDecode.put(c.getKey(), new HashSet<>());
            }
            filtersToDecode.get(c.getKey()).add(c.getValue());
        }

        // generate candidate symFilterTrees for each candidate
        List<SymFilterCompTree> candidateTrees = new ArrayList<>();
        for (Map.Entry<AbstractSymbolicTable, Set<SymbolicFilter>> c : filtersToDecode.entrySet()) {
            Map<SymbolicFilter, List<SymFilterCompTree>> cQuery = c.getKey().batchGenDecomposition(c.getValue());
            for (Map.Entry<SymbolicFilter, List<SymFilterCompTree>> i : cQuery.entrySet()) {
                candidateTrees.addAll(i.getValue());
            }
        }

        List<TableNode> unrankedResult = new ArrayList<>();
        System.out.println(candidateTrees.size());

        for (SymFilterCompTree t : candidateTrees) {
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
                //System.out.println("Find one here!");
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
