package enumerator.tableenumerator;

import main.Synthesizer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by clwang on 3/24/16.
 */
public class CanonicalWithoutExistsEnumeratorTest {

    @Test
    public void test1() {
        Synthesizer.Synthesize("data//StackOverflow//001", new CanonicalWithoutExistsEnumerator());
    }

    @Test
    public void test2() {
        Synthesizer.Synthesize("data//StackOverflow//002", new CanonicalWithoutExistsEnumerator());
    }

    @Test
    public void test3() {
        Synthesizer.Synthesize("data//StackOverflow//008", new CanonicalWithoutExistsEnumerator());
    }

    @Test
    public void test4() { Synthesizer.Synthesize("data//StackOverflow//022", new CanonicalWithoutExistsEnumerator()); }
}