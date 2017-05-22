package forward_enumeration.staged_enumerator;

import forward_enumeration.context.EnumContext;
import forward_enumeration.QueryContainer;
import forward_enumeration.AbstractTableEnumerator;
import global.GlobalConfig;
import backward_inference.MappingInference;
import global.Statistics;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.predicate.EmptyPred;
import lang.sql.ast.contable.SelectNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.ast.valnode.NamedVal;
import summarytable.*;
import util.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/28/16.
 * The enumeration algorithm with abstract table representation.
 */
public class StagedEnumerator extends AbstractTableEnumerator {

    @Override
    public List<TableNode> enumTable(EnumContext ec, int depth) {

        // this is the list to store all summary tables used in enumeration
        List<AbstractSummaryTable> summaryTables = new ArrayList<>();

        // construct symbolic table for each named table.
        QueryContainer candidateCollector = new QueryContainer(QueryContainer.ContainerType.SummaryTableWBV);
        // add a reference
        candidateCollector.allSummaryTables = summaryTables;

        //##### Synthesize SPJ queries
        // build symbolic table for each input table, and store them in SymTables
        List<AbstractSummaryTable> inputSummary = StagedEnumHelper.enumFromInputTables(ec, true);
        summaryTables.addAll(inputSummary);

        if (GlobalConfig.PRINT_LOG)
            System.out.println("[Basic]: " + candidateCollector.getMemoizedTables().size()
                + " [SymTableForInputs]: Intermediate: " + inputSummary.size());

        //##### Synthesize AGGR
        List<AbstractSummaryTable> aggrSummary = StagedEnumHelper.enumAggr(inputSummary, ec);
        summaryTables.addAll(aggrSummary);
        if (GlobalConfig.PRINT_LOG)
            System.out.println("[Aggregation]: " + aggrSummary.size() + " [SymTable]: " + summaryTables.size());

        // only contains symbolic table generated from last stage
        //  (here includes those from aggr and input)
        List<AbstractSummaryTable> basicAndAggr = new ArrayList<>();
        basicAndAggr.addAll(summaryTables);

        //##### Synthesize natural join
        // Natural join all input summary tables
        AbstractSummaryTable naturalJoinResult = StagedEnumHelper
                .naturalJoinWithUnused(Optional.empty(), inputSummary);
        summaryTables.add(naturalJoinResult);

        if (depth == 0) {

            tryEvalToOutput(basicAndAggr, ec, candidateCollector);

            if (GlobalConfig.TRY_NATURAL_JOIN && inputSummary.size() > 1) {
                // try join all tables and infer whether the output table can be obtained in this way
                if (GlobalConfig.PRINT_LOG)
                    System.out.println("[NaturalJoin]: " + 1 + " [SymTable]: " + summaryTables.size());
                tryEvalToOutput(naturalJoinResult, ec, candidateCollector);
            }
            return decodeToQueries(candidateCollector, ec);
        }

        // only try eval the queries at this depth
        tryEvalToOutput(basicAndAggr, ec, candidateCollector);
        if (candidateCollector.getAllCandidates().size() > 0) {
            return decodeToQueries(candidateCollector, ec);
        }

        //##### Synthesize JOIN
        List<AbstractSummaryTable> stFromLastStage = basicAndAggr;
        List<AbstractSummaryTable> joinSummary;
        for (int i = 1; i <= depth; i ++) {

            joinSummary = StagedEnumHelper.enumJoin(stFromLastStage, inputSummary);
            summaryTables.addAll(joinSummary);
            stFromLastStage = joinSummary;

            if (GlobalConfig.PRINT_LOG)
                System.out.println("[JOIN] level " + i + " [SymTable]: " + summaryTables.size());
        }
        // only try eval the queries at this depth
        if (!stFromLastStage.equals(basicAndAggr))
            tryEvalToOutput(stFromLastStage, ec, candidateCollector);
        if (candidateCollector.getAllCandidates().size() > 0) {
            return decodeToQueries(candidateCollector, ec);
        }

        //##### Synthesize UNION
        List<AbstractSummaryTable> unionSummary;
        stFromLastStage = inputSummary;
        for (int i = 1; i <= depth-1; i ++) {
            unionSummary = StagedEnumHelper.enumUnion(stFromLastStage, inputSummary);
            summaryTables.addAll(unionSummary);
            stFromLastStage = unionSummary;
            if (GlobalConfig.PRINT_LOG)
                System.out.println("[UNION] level " + i + " [SymTable]: " + summaryTables.size());
        }
        if (!stFromLastStage.equals(inputSummary))
            tryEvalToOutput(stFromLastStage, ec, candidateCollector);
        if (candidateCollector.getAllCandidates().size() > 0) {
            return decodeToQueries(candidateCollector, ec);
        }
        //##### Synthesize LEFT-JOIN
        // Try enumerating joining two tables from left-join
        stFromLastStage = inputSummary;
        for (int i = 1; i <= depth-1; i ++) {

            List<AbstractSummaryTable> leftJoinSummary = StagedEnumHelper.enumLeftJoin(stFromLastStage, inputSummary, ec);
            List<AbstractSummaryTable> rightJoinSummary = StagedEnumHelper.enumLeftJoin(inputSummary, stFromLastStage, ec);
            leftJoinSummary.addAll(rightJoinSummary);
            stFromLastStage = leftJoinSummary;

            summaryTables.addAll(stFromLastStage);

            if (GlobalConfig.PRINT_LOG)
                System.out.println("[EnumLeftJoin] level " + i + " [SymTable]: " + summaryTables.size());
        }
        if (!stFromLastStage.equals(inputSummary)) {
            tryEvalToOutput(stFromLastStage, ec, candidateCollector);
            if (candidateCollector.getAllCandidates().size() > 0) {
                return decodeToQueries(candidateCollector, ec);
            }
        }
        // Try enumerating joining two tables from left-join with aggregation
        stFromLastStage = aggrSummary;
        for (int i = 1; i <= depth-1; i ++) {

            List<AbstractSummaryTable> leftJoinSummary = StagedEnumHelper
                    .enumLeftJoin(stFromLastStage, inputSummary, ec);

            List<AbstractSummaryTable> rightJoinSummary = StagedEnumHelper
                    .enumLeftJoin(inputSummary, stFromLastStage, ec);

            leftJoinSummary.addAll(rightJoinSummary);
            stFromLastStage = leftJoinSummary;

            summaryTables.addAll(stFromLastStage);
            if (GlobalConfig.PRINT_LOG)
                System.out.println("[EnumLeftJoinWAggr] level " + i + " [SymTable]: " + summaryTables.size());

        }
        if (! stFromLastStage.equals(aggrSummary)) {
            tryEvalToOutput(stFromLastStage, ec, candidateCollector);
            if (candidateCollector.getAllCandidates().size() > 0) {
                return decodeToQueries(candidateCollector, ec);
            }
        }

        if (candidateCollector.getAllCandidates().size() > 0) {
            return decodeToQueries(candidateCollector, ec);
        }
        //##### Synthesize aggregation on join
        // Enumerate aggregation on joined tables
        List<AbstractSummaryTable> simpleJoinSummary = inputSummary;
        for (int i = 1; i <= depth - 1; i ++) {
            simpleJoinSummary = StagedEnumHelper.enumJoin(simpleJoinSummary, inputSummary);
            summaryTables.addAll(simpleJoinSummary);
        }
        if (!simpleJoinSummary.equals(inputSummary)) {
            // also include natural join result for depth 2
            if (depth == 2)
                simpleJoinSummary.add(naturalJoinResult);
            List<AbstractSummaryTable> aggrOnJoinSummary = StagedEnumHelper.enumAggr(simpleJoinSummary, ec);
            summaryTables.addAll(aggrOnJoinSummary);

            if (depth > 2) {
                List<AbstractSummaryTable> joinAfterAggr = StagedEnumHelper.enumJoin(aggrOnJoinSummary, inputSummary);
                summaryTables.addAll(joinAfterAggr);
                tryEvalToOutput(joinAfterAggr, ec, candidateCollector);
            } else {
                tryEvalToOutput(aggrOnJoinSummary, ec, candidateCollector);
            }

            if (GlobalConfig.PRINT_LOG)
                System.out.println("[EnumAggrOnJoin] " + " [SymTable]: " + summaryTables.size());

            if (candidateCollector.getAllCandidates().size() > 0) {
                return decodeToQueries(candidateCollector, ec);
            }
        }

        //##### Synthesize join with aggregation result
        // Try enumerating by joining two tables from aggregation
        stFromLastStage = basicAndAggr;
        for (int i = 1; i <= depth; i ++) {
            List<AbstractSummaryTable> tmp = StagedEnumHelper.enumJoin(stFromLastStage, basicAndAggr);
            summaryTables.addAll(tmp);
            stFromLastStage = tmp;
            if (GlobalConfig.PRINT_LOG)
                System.out.println("[EnumJoinOnAggr] level " + i + " [SymTable]: " + summaryTables.size());
        }
        if (!stFromLastStage.equals(basicAndAggr)) {
            tryEvalToOutput(stFromLastStage, ec, candidateCollector);
            if (candidateCollector.getAllCandidates().size() > 0) {
                return decodeToQueries(candidateCollector, ec);
            }
        }

        //##### Synthesizing aggregation on aggregation
        List<AbstractSummaryTable> aggrAggrSummary = StagedEnumHelper.enumAggr(aggrSummary, ec);
        for (int i = 1; i <= depth - 1; i ++) {
            List<AbstractSummaryTable> tmp = StagedEnumHelper.enumJoin(aggrAggrSummary, basicAndAggr);
            aggrAggrSummary = tmp;
            summaryTables.addAll(tmp);
            if (GlobalConfig.PRINT_LOG)
                System.out.println("[EnumAggrOnAggr Then Join] level " + i + " [SymTable]: " + summaryTables.size());
        }
        if (depth > 1) tryEvalToOutput(aggrAggrSummary, ec, candidateCollector);

        return decodeToQueries(candidateCollector, ec);
    }

    public List<TableNode> decodeToQueries(QueryContainer qc, EnumContext ec) {

        if (GlobalConfig.STAT_MODE) {
            Set<Table> tables = new HashSet<>();

            for (AbstractSummaryTable st : qc.allSummaryTables) {
                tables.add(st.getBaseTable());
            }

            if (GlobalConfig.PRINT_LOG) {
                System.out.println("[Total Number of Intermediate] " + tables.size());
                System.out.println("[SumTableSize] " + tables.stream()
                        .map(st -> st.getContent().size() * st.getContent().get(0).getValues().size()).reduce(Integer::sum).get());
            }
        }

        // Extract the filters to be decoded for each AbstractSymbolicTable from qc
        Map<AbstractSummaryTable, Set<BVFilter>> filtersToDecode = new HashMap<>();
        for (Pair<AbstractSummaryTable, BVFilter> c : qc.getAllCandidates()) {
            if (! filtersToDecode.containsKey(c.getKey())) {
                filtersToDecode.put(c.getKey(), new HashSet<>());
            }
            filtersToDecode.get(c.getKey()).add(c.getValue());
        }

        // generate candidate symFilterTrees for each candidate
        // use a bucket to store queries generated from the skeletons
        // each bucket contains all queries constructed from a candidate summary table
        int candidateTreeCount = 0;
        Map<AbstractSummaryTable, List<TableNode>> candidateBuckets = new HashMap<>();

        for (Map.Entry<AbstractSummaryTable, Set<BVFilter>> c : filtersToDecode.entrySet()) {
            Map<BVFilter, List<BVFilterCompTree>> cQuery = c.getKey().batchGenDecomposition(c.getValue());
            candidateBuckets.put(c.getKey(), new ArrayList<>());

            candidateTreeCount += cQuery.values().size();

            List<BVFilterCompTree> bvCompTrees = new ArrayList<>();
            for (Map.Entry<BVFilter, List<BVFilterCompTree>> i : cQuery.entrySet()) {
                bvCompTrees.addAll(i.getValue());
            }
            List<TableNode> tempCandidateQueries = new ArrayList<>();
            for (BVFilterCompTree bvt : bvCompTrees) {
                tempCandidateQueries.addAll(bvt.translateToTopSQL(ec));
            }
            tempCandidateQueries.sort((x,y) -> (Double.compare(x.estimateTotalScore(ec.getUserProvidedConstValues()),
                    y.estimateTotalScore(ec.getUserProvidedConstValues()))));

            candidateBuckets.get(c.getKey())
                    .addAll(tempCandidateQueries.subList(0, tempCandidateQueries.size() > 10 ? 10 :
                                                            tempCandidateQueries.size()));
        }

        if (GlobalConfig.PRINT_LOG)
            System.out.println("Candidate Tree Number: " + candidateTreeCount);

        // get the top result for each group
        List<TableNode> topTreeDecodeResult = new ArrayList<>();
        List<TableNode> remainingResults = new ArrayList<>();
        for (Map.Entry<AbstractSummaryTable, List<TableNode>> entry : candidateBuckets.entrySet()) {
            topTreeDecodeResult.add(entry.getValue().get(0));
            //remainingResults.addAll(entry.getValue().subList(1, entry.getValue().size()));
        }

        topTreeDecodeResult.sort((x,y) -> (
                Double.compare(x.estimateTotalScore(ec.getUserProvidedConstValues()),
                               y.estimateTotalScore(ec.getUserProvidedConstValues()))));
        remainingResults.sort((x,y) -> (
                Double.compare(x.estimateTotalScore(ec.getUserProvidedConstValues()),
                        y.estimateTotalScore(ec.getUserProvidedConstValues()))));

        // add synthesized queries to the result set
        if (topTreeDecodeResult.size() < 30) {
            int remainingSlotCnt = 30 - topTreeDecodeResult.size();
            topTreeDecodeResult.addAll(remainingResults.subList(0,
                    remainingResults.size() > remainingSlotCnt ? remainingSlotCnt : remainingResults.size()));
        } else {
            topTreeDecodeResult = topTreeDecodeResult.subList(0, 30);
        }

        List<TableNode> decodeResult = new ArrayList<>();
        for (TableNode tn : topTreeDecodeResult) {
            try {
                Table t = tn.eval(new Environment());
                List<Integer> projectionIndexes = Table.inferProjection(t, ec.getOutputTable());

                // with projection result printed
                SelectNode stn;

                if ((tn instanceof SelectNode)) {
                    stn = new SelectNode(
                            projectionIndexes
                                    .stream()
                                    .map(x -> ((SelectNode) tn).getColumns().get(x)).collect(Collectors.toList()),
                            ((SelectNode) tn).getTableNode(),
                            ((SelectNode) tn).getFilter());
                } else {
                    stn = new SelectNode(
                            projectionIndexes
                                    .stream()
                                    .map(x -> new NamedVal(tn.getSchema().get(x))).collect(Collectors.toList()),
                            tn,
                            new EmptyPred());
                }

                decodeResult.add(stn);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        return decodeResult;
    }

    // The following two methods try to infer whether the output example can be derived from summary tables
    // Upon success, candidates will be stored into candidate collector

    private void tryEvalToOutput(List<AbstractSummaryTable> stList, EnumContext ec, QueryContainer candidateCollector) {
        stList.forEach(st -> tryEvalToOutput(st, ec, candidateCollector));
    }

    // Try to evaluate whether the output table can be derived from symbolic table st.
    private void tryEvalToOutput(AbstractSummaryTable st, EnumContext ec, QueryContainer candidateCollector) {

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
            MappingInference mi = MappingInference.buildMapping(t, ec.getOutputTable());
            if( t.getContent().size() == ec.getOutputTable().getContent().size()
                    && mi.searchOneMappingInstance().isPresent()) {
                // Be super careful, the following condition is not sufficient
                // ====>> ! mi.genColumnMappingInstances().isEmpty()
                // We need to use the condition below
                // ====>> mi.searchOneMappingInstance().isPresent()
                candidateCollector.insertCandidate(new Pair<>(symTable, symFilter));
            }
        };

        Statistics.TotalTableTryEvaluated ++;

        MappingInference mi = MappingInference.buildMapping(st.getBaseTable(), ec.getOutputTable());
        if (! mi.everyCellHasImage()) {
            Statistics.PrunedAbstractTableCount ++;
            return;
        }

        st.emitFinalVisitAllTables(mi, ec, f);
    }

}