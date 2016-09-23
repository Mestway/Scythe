package forward_enumeration.table_enumerator;

import scythe_interface.Synthesizer;
import org.testng.annotations.Test;

/**
 * Created by clwang on 4/4/16.
 */
public class SymbolicTableEnumeratorTest {
    @Test
    public void test1() {
        Synthesizer.Synthesize("data//StackOverflow//002", new StagedEnumerator());
    }

}