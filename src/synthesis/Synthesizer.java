package synthesis;

import enumerator.Constraint;
import enumerator.EnumContext;
import enumerator.hueristics.HeuristicTableJoin;
import enumerator.hueristics.TableNaturalJoinWithAggr;
import mapping.CoordInstMap;
import mapping.MappingInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by clwang on 2/20/16.
 */
public class Synthesizer {

    public List<TableNode> synthesize(List<Table> inputs, Table output, Constraint c) throws SQLEvalException {
        TableNode inputNode = HeuristicTableJoin.equiJoin(inputs);
        Table input = inputNode.eval(new Environment());
        EnumContext ec = new EnumContext(Arrays.asList(input), c);

        Table extendedTable = TableNaturalJoinWithAggr
                .naturalTableExtensionWithAggr(ec).eval(new Environment());

        System.out.println(extendedTable);

        MappingInference mi = MappingInference.buildMapping(extendedTable, output);
        mi.refineMapping();

        List<CoordInstMap> instances = mi.genMappingInstances();

        for (CoordInstMap cim : instances) {
            System.out.println("---");
            System.out.println(cim.toString());
            Map<Integer, Integer> lineMap = MappingInference.columnMappingInference(cim);
            for (Map.Entry<Integer, Integer> entry : lineMap.entrySet()) {
                //System.out.println(entry.getKey() + " - " + entry.getValue());
            }
        }

        List<Filter> atomicFilters = mi.atomicFilterEnum(new ArrayList<>(), 0);

        System.out.println("[Atomic Filter Enum Done] size: " + atomicFilters.size());
        MappingInference.filterMemoization(extendedTable, atomicFilters);

        return null;
    }
}
