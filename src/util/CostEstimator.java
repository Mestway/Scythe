package util;

import enumerator.context.EnumContext;
import sql.lang.ast.filter.*;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;

import java.util.List;
import java.util.Map;

/**
 * Created by clwang on 5/19/16.
 */
public class CostEstimator {

    public static float estimateFilterCost(Filter f, Map<String, String> originNameMap) {
        if (f instanceof EmptyFilter) {
            return 0;
        } else if (f instanceof ExistComparator) {
            return 3;
        } else if (f instanceof VVComparator) {
            float score = 0;
            List<ValNode> args = ((VVComparator) f).getArgs();

            if (args.get(0) instanceof ConstantVal || args.get(0) instanceof ConstantVal) {
                score += 0.5;
            } else if (args.get(0) instanceof NamedVal && args.get(1) instanceof NamedVal) {
                if (originNameMap.get(args.get(0).getName()).equals(originNameMap.get(args.get(1).getName()))) {
                    score += 0.5;
                } else {
                    score += 1;
                }
            }

            if (((VVComparator) f).getComparator().equals(VVComparator.eq)) {
                score += 0;
            } else if (((VVComparator) f).getComparator().equals(VVComparator.neq)) {
                score += 2;
            } else {
                score += 1.5;
            }

            return score;
        } else if (f instanceof LogicAndFilter) {
            return estimateConjFilterList(((LogicAndFilter) f).getAllFilters(), originNameMap);
        } else {
            return 3;
        }
    }

    public static float estimateConjFilterList(List<Filter> filters, Map<String, String> originNameMap) {

        float basicScore = filters.stream()
                .map(f -> CostEstimator.estimateFilterCost(f, originNameMap)).reduce((float)0, (x, y) -> x + y);

        float rate = 1;
        for (int i = 0; i < filters.size(); i ++) {
            for (int j = i + 1; j < filters.size(); j ++) {
                rate *= compatibilityRate(filters.get(i), filters.get(j));
            }
        }

        return rate * basicScore;
    }

    private static float compatibilityRate(Filter f1, Filter f2) {
        if (f1 instanceof ExistComparator && f2 instanceof ExistComparator) {
            return 3;
        } else if (f1 instanceof VVComparator && f2 instanceof VVComparator) {
            List<ValNode> f1Args = ((VVComparator) f1).getArgs();
            List<ValNode> f2Args = ((VVComparator) f2).getArgs();
            boolean allEqual = true;
            for (int i = 0; i < f1Args.size(); i ++) {
                if (! f1Args.get(i).getName().equals(f2Args.get(i).getName()))
                    allEqual = false;
            }

            if (allEqual)
                return 4;
        }
        return 1;
    }

    public static float estimateTableNodeCost(TableNode tn, EnumContext ec) {

        return 0;
    }

}
