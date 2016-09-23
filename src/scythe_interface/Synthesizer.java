package scythe_interface;

import forward_enumeration.table_enumerator.AbstractTableEnumerator;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

import java.util.List;

/**
 * Created by clwang on 3/22/16.
 */
public class Synthesizer {

    public static List<TableNode> Synthesize(String path, int maxDepth, AbstractTableEnumerator enumerator) {

        // read file
        ExampleDS exampleDS = ExampleDS.readFromFile(path);
        System.out.println("================\n[[Synthesizer start]] " + path);
        System.out.println("[Enumerator Used] " + enumerator.getClass().getSimpleName());
        long timeStart = System.currentTimeMillis();

        exampleDS.enumConstraint.setMaxDepth(maxDepth);

        // synthesize
        List<TableNode> candidates = enumerator.enumProgramWithIO(exampleDS.inputs, exampleDS.output, exampleDS.enumConstraint);

        int count = 0;
        for (TableNode tn : candidates) {
            try {
                Table t = tn.eval(new Environment());
                if( count >= 20) break;
                System.out.println("[No." + (count + 1) + "]===============================");
                count ++;
                System.out.println(tn.prettyPrint(0));
                System.out.println(t);
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }
        }

        // formatting time
        long timeUsed = System.currentTimeMillis() - timeStart;
        long second = (timeUsed / 1000) % 60;
        long minute = (timeUsed / (1000 * 60)) % 60;
        long hour = (timeUsed / (1000 * 60 * 60)) % 24;
        String time = String.format("%02d:%02d:%02d:%d", hour, minute, second, timeUsed % 1000);

        System.out.println("[[Synthesizer finished]] time: " + time);
        System.out.printf("[[Synthesizer finished]] seconds: %.5f", (minute*60. + second + 0.001 * (timeUsed % 1000)));

        return candidates;
    }
}
