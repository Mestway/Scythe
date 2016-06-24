package util;

import enumerator.context.EnumContext;
import sql.lang.ast.filter.*;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by clwang on 5/19/16.
 */
public class CostEstimator {

    public static double estimateFilterCost(Filter f, Map<String, String> originNameMap) {
        if (f instanceof EmptyFilter) {
            return 0;
        } else if (f instanceof ExistComparator) {
            double cost = 0.5 + 4. * (((ExistComparator) f).getTableNode().estimateAllFilterCost() / 5);
            return cost;
        } else if (f instanceof VVComparator) {
            float score = 0;
            List<ValNode> args = ((VVComparator) f).getArgs();

            if (args.get(0) instanceof ConstantVal || args.get(1) instanceof ConstantVal) {
                score += 0.2;
            } else if (args.get(0) instanceof NamedVal && args.get(1) instanceof NamedVal) {

                String name0 = args.get(0).getName(), name1 = args.get(1).getName();

                String originName0, originName1;
                if (originNameMap.get(args.get(0).getName()) == null) {
                    originName0 = args.get(0).getName();
                } else {
                    originName0 = originNameMap.get(args.get(0).getName());
                }

                if (originNameMap.get(args.get(1).getName()) == null) {
                    originName1 = args.get(1).getName();
                } else {
                    originName1 = originNameMap.get(args.get(1).getName());
                }

                if (originName0.equals(originName1)) {
                    score += 0.5;
                } else if (originName0.substring(originName0.lastIndexOf(".")).equals(originName1.lastIndexOf("."))) {
                    score += 0.7;
                } else {
                    score += 1;
                }

                if (name0.contains("MAX-") || name1.contains("MAX-")
                        || name0.contains("MIN-") || name1.contains("MIN-")) {
                    if (! ((VVComparator) f).getComparator().equals(VVComparator.eq)
                            && !((VVComparator) f).getComparator().equals(VVComparator.neq)) {
                        if (originName0.equals(originName1))
                            score += 20;
                        else {
                            score += 10;
                        }
                    }
                }
            }

            if (((VVComparator) f).getComparator().equals(VVComparator.eq)) {
                score += 0;
            } else if (((VVComparator) f).getComparator().equals(VVComparator.neq)) {
                score += 1.5;
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

    public static double estimateConjFilterList(List<Filter> filters, Map<String, String> originNameMap) {
        double basicScore = filters.stream()
                .map(f -> CostEstimator.estimateFilterCost(f, originNameMap)).reduce(0., (x, y) -> x + y);

        float rate = 1;
        for (int i = 0; i < filters.size(); i ++) {
            for (int j = i + 1; j < filters.size(); j ++) {
                rate *= compatibilityRate(filters.get(i), filters.get(j));
            }
        }

        return rate * basicScore;
    }

    private static double compatibilityRate(Filter f1, Filter f2) {
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

    public static double estimateTableNodeCost(TableNode tn, EnumContext ec) {
        return 0;
    }

    public static double estimateExistsFilterCost(ExistComparator efilter) {
        return efilter.getTableNode().estimateAllFilterCost();
    }

}
