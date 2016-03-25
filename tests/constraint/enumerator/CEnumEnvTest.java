package constraint.enumerator;

import constraint.lang.filter.CBiComparator;
import constraint.lang.filter.CFilter;
import org.junit.Test;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by clwang on 3/7/16.
 */
public class CEnumEnvTest {

    @Test
    public void test() {
        String inputSrc =
                "| id | conversation_id | name  | timestamp | content |" + "\r\n" +
                "|-----------------------------------|" + "\r\n" +
                "| 2  | 145  | xxx | 10000 | message |" + "\r\n" +
                "| 6  | 1743 | yyy | 999   | message |" + "\r\n" +
                "| 7  | 14   | bbb | 899   | message |" + "\r\n" +
                "| 1  | 145  | xxx | 10000 | message |" + "\r\n" +
                "| 5  | 1743 | me  | 1200  | message |";

        String outputSrc =
                "| c1 | c2   |  c3 | c4    |  c5     |" + "\r\n" +
                "|-----------------------------------|" + "\r\n" +
                "| 2  | 145  | xxx | 10000 | message |" + "\r\n" +
                "| 6  | 1743 | yyy | 999   | message |" + "\r\n" +
                "| 7  | 14   | bbb | 899   | message |";

        Table input = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
        Table output = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

        List<Function<List<Value>, Value>> aggrFuncs = Arrays.asList(AggregationNode.AggrMax);
        List<Value> constants = Arrays.asList(Value.parse("me"));

        CEnumEnv cee = new CEnumEnv(input, output, constants, aggrFuncs);

        /*cee.enumerateCBiComparator();
        List<CBiComparator> bifuncs = cee.getPrimitiveComparators();
        for (CBiComparator cbc : bifuncs) {
            System.out.println(cbc.toString());
        }*/
        cee.enumerateFilters();
        int trueCount = 0;
        for (CFilter cf : cee.getFilters()) {
            String s = cf.toString();
            boolean conflict = false;
            for (int i = 0; i < cf.getComparators().size(); i ++) {
                for (int j = i + 1; j < cf.getComparators().size(); j ++) {
                    if (CBiComparator.conflict(cf.getComparators().get(i), cf.getComparators().get(j))) {
                        conflict = true;
                        break;
                    }
                }
            }
            s += " " + conflict;
            if (conflict == true) trueCount ++;
            //System.out.println(s);
        }
        System.out.println("True Count: " + trueCount);
        System.out.println(cee.getFilters().size());
    }

}