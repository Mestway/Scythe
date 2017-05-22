package forward_enumeration.staged_enumerator;

import forward_enumeration.enum_components.AggrEnumerator;
import forward_enumeration.enum_components.LeftJoinEnumerator;
import forward_enumeration.context.EnumContext;
import global.GlobalConfig;
import lang.sql.ast.Environment;
import lang.sql.ast.contable.NamedTableNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.exception.SQLEvalException;
import lang.table.Table;
import summarytable.AbstractSummaryTable;
import summarytable.BVFilter;
import summarytable.CompoundSummaryTable;
import summarytable.PrimitiveSummaryTable;
import util.Pair;
import util.RenameWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by clwang on 5/22/17.
 */
public class StagedEnumHelper {

    public static List<AbstractSummaryTable> enumFromInputTables(EnumContext ec, boolean allowDisj) {
        return ec.getInputs()
                .stream().map(t -> new PrimitiveSummaryTable(t, new NamedTableNode(t), allowDisj))
                .collect(Collectors.toList());
    }

    public static List<AbstractSummaryTable> enumAggr(List<AbstractSummaryTable> stList, EnumContext ec) {

        List<AbstractSummaryTable> result = new ArrayList<>();

        for (AbstractSummaryTable st : stList) {

            // core tables to be used in enumerate aggregation
            List<Pair<Table, BVFilter>> instantiated = st.instantiatedAllTablesWithBVFilters(ec);

            for (Pair<Table, BVFilter> p : instantiated) {
                // enumerating aggregation queries,
                // the core of each query is built atop a filtering clause on an input table
                ec.setTableNodes(Arrays.asList(new NamedTableNode(p.getKey())));
                List<TableNode> ans = AggrEnumerator.enumAggrFromEC(ec, GlobalConfig.SIMPLIFY_AGGR_FIELD);
                for (TableNode an : ans) {
                    try {
                        // build symbolic tables out of aggregation table nodes,
                        // and store them for next stage enumeration
                        result.add(new PrimitiveSummaryTable(
                                an.eval(new Environment()),
                                an,
                                Arrays.asList(new Pair<>(st, p.getValue()))));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result;
    }

    public static List<AbstractSummaryTable> enumJoin(
            List<AbstractSummaryTable> leftStList,
            List<AbstractSummaryTable> rightStList) {

        // used to collect queries generated in the most recent stage
        List<AbstractSummaryTable> collector = new ArrayList<>();
        for (int k = 0; k < leftStList.size(); k ++) {
            for (int l = 0; l < rightStList.size(); l ++) {
                AbstractSummaryTable st1 = leftStList.get(k);
                AbstractSummaryTable st2 = rightStList.get(l);
                CompoundSummaryTable sct = new CompoundSummaryTable(st1, st2,
                        CompoundSummaryTable.CompositionType.join);
                collector.add(sct);
            }
        }

        return collector;
    }

    public static List<AbstractSummaryTable> enumUnion(
            List<AbstractSummaryTable> leftStList,
            List<AbstractSummaryTable> rightStList) {

        List<AbstractSummaryTable> collector = new ArrayList<>();
        for (int k = 0; k < leftStList.size(); k ++) {
            for (int l = 0; l < rightStList.size(); l ++) {

                AbstractSummaryTable st1 = leftStList.get(k);
                AbstractSummaryTable st2 = rightStList.get(l);

                if (! Table.schemaMatch(st1.getBaseTable(), st2.getBaseTable()))
                    continue;

                CompoundSummaryTable sct = new CompoundSummaryTable(st1, st2,
                        CompoundSummaryTable.CompositionType.union);
                collector.add(sct);
            }
        }
        return collector;
    }

    public static List<AbstractSummaryTable> enumLeftJoin(
            List<AbstractSummaryTable> leftStList,
            List<AbstractSummaryTable> rightStList,
            EnumContext ec) {

        // we don't eval left table since it will be filtered
        //      when inferring filtering on the primitive summary table
        List<Pair<Table, Pair<AbstractSummaryTable, BVFilter>>> leftTablePairs = leftStList.stream()
                .map(st -> new Pair<>(st.getBaseTable(),
                        new Pair<>(st, BVFilter.genEmptyFilter(st.getBaseTable().getContent().size()))))
                .collect(Collectors.toList());
        List<Pair<Table, Pair<AbstractSummaryTable, BVFilter>>> rightTablePairs = rightStList.stream()
                .flatMap(st -> st.instantiatedAllTablesWithBVFilters(ec)
                        .stream()
                        .map(p -> new Pair<>(p.getKey(), new Pair<>(st, p.getValue()))))
                .collect(Collectors.toList());

        List<AbstractSummaryTable> result = new ArrayList<>();

        for (Pair<Table, Pair<AbstractSummaryTable, BVFilter>> p1 : leftTablePairs) {
            for (Pair<Table, Pair<AbstractSummaryTable, BVFilter>> p2 : rightTablePairs) {

                List<TableNode> tns =
                        LeftJoinEnumerator
                                .enumLeftJoin(new NamedTableNode(p1.getKey()), new NamedTableNode(p2.getKey()))
                                .stream()
                                .map(RenameWrapper::tryRename).collect(Collectors.toList());

                List<Pair<AbstractSummaryTable, BVFilter>> subTree = new ArrayList<>();
                subTree.add(p1.getValue());
                subTree.add(p2.getValue());

                for (TableNode tn : tns) {
                    try {
                        result.add(new PrimitiveSummaryTable(tn.eval(new Environment()), tn, subTree));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result;
    }

    // for each table on the left, natural join them with some table on the right side
    public static List<AbstractSummaryTable> enumNaturalJoin(List<AbstractSummaryTable> current,
                                                             List<AbstractSummaryTable> inputSummary) {
        List<AbstractSummaryTable> result = new ArrayList<>();

        for (AbstractSummaryTable st : current) {
            AbstractSummaryTable r = naturalJoinWithUnused(Optional.of(st), inputSummary);
            if (! r.equals(st))
                result.add(r);
        }

        return result;
    }

    // looking into allBasicTables for those not yet used and then perform a natural join
    public static AbstractSummaryTable naturalJoinWithUnused(Optional<AbstractSummaryTable> current,
                                                             List<AbstractSummaryTable> inputSummary) {

        List<AbstractSummaryTable> unusedSummary = new ArrayList<>();

        if (current.isPresent()) {
            unusedSummary.add(current.get());
            unusedSummary.addAll(inputSummary.stream().filter(st -> {
                if (! current.get().namedTableInvolved()
                        .stream().map(nt -> nt.getTable())
                        .collect(Collectors.toSet())
                        .contains(st.getBaseTable())) {
                    return true;
                }
                return false;
            }).collect(Collectors.toList()));
        } else {
            unusedSummary = inputSummary;
        }

        if (unusedSummary.isEmpty()) {
            System.out.println("[[ERROR StagedEnumerator316]] no table exists in join.");
        }

        if (unusedSummary.size() == 1)
            return unusedSummary.get(0);

        AbstractSummaryTable naturalJoinResult = unusedSummary.get(0);
        for (int i = 1; i < unusedSummary.size(); i ++) {
            naturalJoinResult = new CompoundSummaryTable(naturalJoinResult, unusedSummary.get(i),
                    CompoundSummaryTable.CompositionType.join);
        }
        return naturalJoinResult;
    }

}
