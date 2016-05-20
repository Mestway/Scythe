package util;

import enumerator.context.EnumContext;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.ExistComparator;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.filter.VVComparator;
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

    public static float estimateFilterCost(Filter f, Map<String, String> originNameMapper) {
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
                if (originNameMapper.get(args.get(0).getName()).equals(originNameMapper.get(args.get(1).getName()))) {
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
        } else {
            return 3;
        }
    }

    public static float estimateTableNodeCost(TableNode tn, EnumContext ec) {

        return 0;
    }

}
