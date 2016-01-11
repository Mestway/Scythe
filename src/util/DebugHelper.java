package util;

import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

import java.util.List;

/**
 * Created by clwang on 1/7/16.
 */
public class DebugHelper {
    static int count = 0;
    public static void debugPrintTableNode(TableNode tn, String debugInfo) {
        System.out.println(count ++);
        /*System.out.println(" ------ ");
        System.out.println(debugInfo);
        System.out.print(tn.prettyPrint(0));
        System.out.println();*/
    }

    public static void printTableNodes(List<TableNode> tns) {
        for (TableNode tn  : tns) {
            try {
                Table tb = tn.eval(new Environment());
                System.out.println("========================");
                System.out.println(tn.prettyPrint(0));
                System.out.println(tb);
            } catch (SQLEvalException e) {
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-");
                System.out.println(tn.prettyPrint(0));;
            }
        }
    }
}
