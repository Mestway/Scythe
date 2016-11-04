package forward_enumeration.table_enumerator;

import org.junit.Test;
import scythe_interface.Synthesizer;

/**
 * Created by clwang on 4/4/16.
 */
public class SymbolicTableEnumeratorTest {
    @Test
    public void test1() {
        Synthesizer.Synthesize("data//StackOverflow//002M", new StagedEnumerator());
    }

}