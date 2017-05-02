package backward_inference.vsa;

import backward_inference.CellIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by clwang on 3/17/17.
 */
public abstract class VSAMapping {

    CellIndex source;


    // cross merge with another node to generate a new node that contains information for both in a consistent fashion
    public abstract Optional<VSAMapping> crossMerge(VSAMapping node);

    // Given two lists of VSANode, merge them
    public static List<VSAMapping> crossMerge(List<VSAMapping> l1, List<VSAMapping> l2) {
        List<VSAMapping> result = new ArrayList<>();
        for (VSAMapping n1 : l1) {
            for (VSAMapping n2 : l2) {
                n1.crossMerge(n2).ifPresent((x) -> result.add(x));
            }
        }
        return result;
    }

}
