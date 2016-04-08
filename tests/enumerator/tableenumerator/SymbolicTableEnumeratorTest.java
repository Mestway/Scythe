package enumerator.tableenumerator;

import main.Synthesizer;
import org.testng.annotations.Test;

import static org.junit.Assert.*;

/**
 * Created by clwang on 4/4/16.
 */
public class SymbolicTableEnumeratorTest {
    @Test
    public void test1() {
        Synthesizer.Synthesize("data//StackOverflow//002", 2, new SymbolicTableEnumerator());
    }

}