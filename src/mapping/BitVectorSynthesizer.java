package mapping;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Created by clwang on 2/22/16.
 */
public class BitVectorSynthesizer {

    int maxLength = 5;
    List<BitSet> encodings = new ArrayList<>();
    BitSet desired;

    public BitVectorSynthesizer(List<BitSet> encodings, BitSet desired) {
        this.encodings = encodings;
        this.desired = desired;
    }

    void dfsSearch(int currentLevel,
                   int maxLevel,
                   List<Integer> current,
                   List<List<Integer>> result) {
        if (currentLevel >= maxLevel) {

        }
    }


}
