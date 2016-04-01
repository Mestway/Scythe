package enumerator.tableenumerator;

import main.Synthesizer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by clwang on 3/31/16.
 */
public class CanonicalTableEnumeratorOnTheFlyTest {
    @Test
    public void test2() {
        Synthesizer.Synthesize("data//StackOverflow//001", new CanonicalTableEnumeratorOnTheFly());
    }
}