package util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by clwang on 1/4/16.
 */
public class IndentionManagementTest {

    @Test
    public void testAddIndention() throws Exception {
        System.out.println(IndentionManagement.addIndention("SELECT\r\n gogo", 3));
    }
}