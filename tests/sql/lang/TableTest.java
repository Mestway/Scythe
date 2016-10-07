package sql.lang;

import org.junit.Assert;
import org.junit.Test;
import sql.lang.datatype.Value;
import util.TableInstanceParser;

import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 10/6/16.
 */
public class TableTest {

    String inputSrc =
            "| locId  |    dtg                |  temp |" + "\r\n" +
                    "|----------------------------------------|" + "\r\n" +
                    "| 100    |  2009-02-25 10:00:00  |  15   |" + "\r\n" +
                    "| 200    |  2009-02-25 10:00:00  |  20   |" + "\r\n" +
                    "| 300    |  2009-02-25 10:00:00  |  24   |" + "\r\n" +
                    "| 100    |  2009-02-25 09:45:00  |  13   |" + "\r\n" +
                    "| 300    |  2009-02-25 09:45:00  |  16   |" + "\r\n" +
                    "| 200    |  2009-02-25 09:45:00  |  18   |" + "\r\n" +
                    "| 400    |  2009-02-25 09:45:00  |  12   |" + "\r\n" +
                    "| 100    |  2009-02-25 09:30:00  |  11   |" + "\r\n" +
                    "| 300    |  2009-02-25 09:30:00  |  14   |" + "\r\n" +
                    "| 200    |  2009-02-25 09:30:00  |  15   |" + "\r\n" +
                    "| 400    |  2009-02-25 09:30:00  |  10   |";

    String outputSrc =
            "| locID |    dtg                  | tmp |" + "\r\n" +
                    "|---------------------------------------|" + "\r\n" +
                    "| 100   |   2009-02-25 10:00:00   | 15  |" + "\r\n" +
                    "| 200   |   2009-02-25 10:00:00   | 20  |" + "\r\n" +
                    "| 300   |   2009-02-25 10:00:00   | 24  |";

    Table t1 = TableInstanceParser.parseMarkDownTable("table1", inputSrc);
    Table t2 = TableInstanceParser.parseMarkDownTable("table2", outputSrc);

    public void testExcept() throws Exception {
        System.out.println(Table.except(t1, t2));
    }

    @Test
    public void testProj() throws Exception {
        System.out.println(t1.projection(Arrays.asList(1, 2)));

    }

}
