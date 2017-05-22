package forward_enumeration.enumerator;

import forward_enumeration.baselines.CanonicalWithoutExistsEnumerator;
import scythe_interface.Synthesizer;
import org.junit.Test;

/**
 * Created by clwang on 3/24/16.
 */
public class CanonicalWithoutExistsEnumeratorTest {

    @Test
    public void test1() {
        Synthesizer.Synthesize("data//StackOverflow//001R", new CanonicalWithoutExistsEnumerator());
    }

    @Test
    public void test2() {
        Synthesizer.Synthesize("data//StackOverflow//002M", new CanonicalWithoutExistsEnumerator());
    }

    @Test
    public void test3() {
        Synthesizer.Synthesize("data//StackOverflow//008", new CanonicalWithoutExistsEnumerator());
    }

    @Test
    public void test4() { Synthesizer.Synthesize("data//StackOverflow//022M", new CanonicalWithoutExistsEnumerator()); }
}