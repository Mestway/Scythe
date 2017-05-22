package util;

import lang.table.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by clwang on 12/11/15.
 * The parser for table examples
 */
public class TableExampleParser {

    // Try to dispatch a parser to parse the table,
    // the format we are able to support are only markdown style or csv table
    public static Table tryParseTable(String tableName, List<String> input) {
        if (input.get(0).contains("|"))
            return parseMarkdownTable(tableName, input);
        else
            return parseCSV(tableName, input);
    }

    /**
     * Parse a table from a list of strings representing rows of the table
     * @param input: a list representing rows of a table:
     *             the first row is the metadata and the content starting from the third row
     * @return a table instance parsed from the given input strings
     */
    public static Table parseMarkdownTable(String tableName, List<String> input) {
        List<List<String>> content = new ArrayList<List<String>>();
        List<String> metadata = splitLineToList(input.get(0));

        int startRow = 1;
        if (Pattern.matches("\\|-*\\|", input.get(startRow).trim()))
            startRow = 2;

        for (int i = startRow; i < input.size(); i ++)
            content.add(splitLineToList(input.get(i)));

        return new Table(tableName, metadata, content);
    }
    public static Table parseMarkDownTable(String tableName, String tableSrc) {
        String[] srclines = tableSrc.split("\r\n");
        List<String> lines = new ArrayList<String>();
        for (String s : srclines) {
            lines.add(s);
        }
        return parseMarkdownTable(tableName, lines);
    }


    public static Table parseCSV(String tableName, List<String> lines) {
        // the first line is the schema of the table
        List<String> schema = splitByComma(lines.get(0));
        List<List<String>> content = new ArrayList<>();
        for (int i = 1; i < lines.size(); i ++) {
            content.add(splitByComma(lines.get(i)));
        }
        return new Table(tableName, schema, content);
    }

    // split a line separated by '|' into a list
    private static List<String> splitLineToList(String line) {
        String[] content = line.trim().split("\\|");
        List<String> list = new ArrayList<String>();
        // range from 1 to size-1 because the first slot is an empty one
        for (int i = 1; i < content.length; i ++) {
            list.add(content[i].trim());
        }
        return list;
    }
    // split a line by comma into a list
    private static List<String> splitByComma(String line) {
        String[] content = line.trim().split(",");
        List<String> lst = new ArrayList<>();
        for (String s : content) lst.add(s.trim());
        return lst;
    }
}
