package util;

import entity.ExampleInstance;
import sql.lang.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 12/11/15.
 */
public class ExampleInstanceReader {
    /**
     * Parse strings into an example instance
     * @param description descriptions
     * @param inputs input tables in a string
     * @param output output table source
     * @return an example instance
     */
    private static ExampleInstance parseExample(String description, String inputs, String output) {

        // input tables after parsing
        List<Table> inputTables = new ArrayList<Table>();
        // output table
        Table outputTable;

        // parsing input tables
        String[] inputlines = inputs.split("\r\n");
        boolean startTable = false;
        List<String> currentContent = new ArrayList<String>();

        // The default table name would be "table"+tableCount
        String currentTableName = "inputTable"; int tableCount = 0;

        for (String l : inputlines) {

            // This line contains the name of the table
            if (l.trim().startsWith("@")) {
                currentTableName = l.trim().substring(1);
            }

            if (l.trim().startsWith("|")) {
                // add each row into the table content for parsing
                currentContent.add(l);
            } else {
                if (!currentContent.isEmpty()) {
                    String name;
                    if (currentTableName.equals("inputTable")) {
                        name = currentTableName + tableCount;
                    } else {
                        name = currentTableName;
                    }
                    tableCount ++;
                    inputTables.add(TableInstanceParser.parseMarkdownTable(name, currentContent));
                    currentContent.clear();
                }
            }
        }
        // process the last instance in the example list
        if (!currentContent.isEmpty()) {
            String name;
            if (currentTableName.equals("inputTable")) {
                name = currentTableName + tableCount;
            } else {
                name = currentTableName;
            }
            inputTables.add(TableInstanceParser.parseMarkdownTable(name, currentContent));
            currentContent.clear();
        }

        // parsing output table, add each content line into the list and parse
        currentTableName = "outputTable";
        String[] outputLines = output.split("\r\n");
        List<String> outputContent = new ArrayList<String>();
        for (String l : outputLines) {

            // Extract the table name
            if (l.startsWith("@")) {
                currentTableName = l.trim().substring(1);
            }

            // If a line starts with "|", it is part of a table
            if (l.trim().startsWith("|")) {
                outputContent.add(l);
            }
        }
        outputTable = TableInstanceParser.parseMarkdownTable(currentTableName, outputContent);

        return new ExampleInstance(description, inputTables, outputTable);
    }

    /**
     * read and parse user input/output/description examples from a markdown file
     * @param lines raw string reading from a md file
     * @return a list of examples instances
     */
    public static List<ExampleInstance> readFromMarkdown(List<String> lines) {

        List<ExampleInstance> examples = new ArrayList<ExampleInstance>();

        // split them into small chunks
        List<String> rawInstances = new ArrayList<String>();
        String lastInstance = "";
        for (String l : lines) {
            if (l.startsWith("# ")) {
                if (!lastInstance.equals("")) {
                    rawInstances.add(lastInstance);
                    lastInstance = "";
                }
            }
            lastInstance += l + "\r\n";
        }
        // We will ignore the last case

        // process each chunk
        for (String rawChunk : rawInstances) {
            String [] chunk = rawChunk.split("\r\n");
            String desc = "";
            String inputE = "";
            String outputE = "";

            for (int i = 0; i < chunk.length; ) {
                String currentLine = chunk[i];
                if (currentLine.startsWith("#### description")) {
                    i ++;
                    while(i < chunk.length && !chunk[i].startsWith("#")) {
                        desc += chunk[i] + "\r\n";
                        i ++;
                    }
                    continue;
                } else if (currentLine.startsWith("#### input")) {
                    i ++;
                    while(i < chunk.length && !chunk[i].startsWith("#")) {
                        inputE += chunk[i] + "\r\n";
                        i ++;
                    }
                    continue;
                } else if (currentLine.startsWith("#### output")) {
                    i ++;
                    while(i < chunk.length && !chunk[i].startsWith("#")) {
                        outputE += chunk[i] + "\r\n";
                        i ++;
                    }
                    continue;
                }
                i ++;
            }

            if (desc.equals(""))
                continue;

            examples.add(parseExample(desc, inputE, outputE));
        }

        return examples;
    }
}
