package enumerator.tableenumerator;

import main.Synthesizer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by clwang on 3/31/16.
 */
public class CanonicalTableEnumeratorOnTheFlyTest {

    public void test1() {
        //Synthesizer.Synthesize("data//StackOverflow//001", new CanonicalTableEnumeratorOnTheFly());
        for (int i = 1; i <= 63; i ++ ) {
            String s = "";
            if (i <= 9) s = "00" + i;
            else s = "0" + i;
            System.out.println("    @Test(timeout=600000)");
            System.out.println("    public void test" + s + "() {");
            System.out.println("        Synthesizer.Synthesize(\"data//StackOverflow//"+ s + "\", new CanonicalTableEnumeratorOnTheFly());");
            System.out.println("    }");
            System.out.println("");
        }
    }

}