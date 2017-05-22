package lang.sql;

import lang.sql.dataval.Value;
import lang.table.TableRow;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 2/8/16.
 */
public class TableRowTest {

    @Test
    public void testContentRoughlyEquals() throws Exception {
        List<Value> v1 = Arrays.asList(
                Value.parse("02/03/2015"),
                Value.parse("02/03/2015"),
                Value.parse("Good"),
                Value.parse("1"),
                Value.parse("1"),
                Value.parse("1"),
                Value.parse("2"),
                Value.parse("3"));

        List<Value> v2 = Arrays.asList(
                Value.parse("02/03/2015"),
                Value.parse("3"),
                Value.parse("Good"),
                Value.parse("1"),
                Value.parse("02/03/2015"),
                Value.parse("1"),
                Value.parse("2"),
                Value.parse("1"));

        List<Value> v3 = Arrays.asList(
                Value.parse("02/03/2015"),
                Value.parse("3"),
                Value.parse("Good"),
                Value.parse("1"),
                Value.parse("02/03/2015"),
                Value.parse("02/03/2015"),
                Value.parse("2"),
                Value.parse("3"));

        Assert.assertTrue(TableRow.isPermutation(v1, v2));
        Assert.assertTrue(TableRow.isPermutation(v2, v1));
        Assert.assertFalse(TableRow.isPermutation(v1, v3));
    }
}