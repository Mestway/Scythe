package util;

import sql.lang.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 12/11/15.
 */
public class TableInstanceParser {

    /**
     * Parse a table from a list of strings representing rows of the table
     * @param input: a list representing rows of a table:
     *             the first row is the metadata and the content starting from the third row
     * @return a table instance parsed from the given input strings
     */
    public static Table parseMarkdownTable(String tableName, List<String> input) {
        List<List<String>> content = new ArrayList<List<String>>();
        List<String> metadata = splitLineToList(input.get(0));
        for (int i = 2; i < input.size(); i ++) {
            content.add(splitLineToList(input.get(i)));
        }
        return new Table(tableName, metadata, content);
    }

    // split a line separated by '|' into a list
    private static List<String> splitLineToList(String line) {
        String[] content = line.trim().split("\\|");
        List<String> list = new ArrayList<String>();
        boolean isBoundaryColumn = true;
        // range from 1 to size-1 because the first slot is an empty one
        for (int i = 1; i < content.length; i ++) {
            list.add(content[i].trim());
        }
        return list;
    }

    public static Table parseMarkDownTable(String tableName, String tableSrc) {
        String[] srclines = tableSrc.split("\r\n");
        List<String> lines = new ArrayList<String>();
        for (String s : srclines) {
            lines.add(s);
        }
        return parseMarkdownTable(tableName, lines);
    }
}
