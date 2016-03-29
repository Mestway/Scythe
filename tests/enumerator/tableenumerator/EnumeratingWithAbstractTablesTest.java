package enumerator.tableenumerator;

import main.Synthesizer;
import org.testng.annotations.Test;

/**
 * Created by clwang on 3/28/16.
 */
public class EnumeratingWithAbstractTablesTest {

    @Test
    public void test1() {
        Synthesizer.Synthesize("data//StackOverflow//002", new IndexedTableEnumerator());
    }


}