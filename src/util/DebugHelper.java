package util;

import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;

import java.util.List;
import java.util.Objects;

/**
 * Created by clwang on 1/7/16.
 */
public class DebugHelper {


    public static boolean debugFlag = false;

    public static void printTableNodes(List<TableNode> tns) {
        for (TableNode tn  : tns) {
            try {
                System.out.println("========================");
                System.out.println(tn.prettyPrint(0));
                Table tb = tn.eval(new Environment());
                System.out.println(tb);
            } catch (SQLEvalException e) {
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-");
                System.out.println(tn.prettyPrint(0));;
            }
        }
    }

    public static void printList(List l) {
        for (Object t : l) {
            System.out.println("--------------------------");
            System.out.println(t.toString());
        }
    }

    public static void printListHorizontal(List l) {
        String result = "";
        for (Object o : l) {
            result += o.toString();
        }
        System.out.println(result);
    }

    public static void printValNodes(List<ValNode> vns) {
        for (ValNode vn : vns) {
            System.out.println("---------------------------");
            System.out.println(vn.prettyPrint(0));
        }
    }
}
