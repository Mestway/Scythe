package entity;

import sql.lang.Table;
import util.TableInstanceParser;

import java.util.ArrayList;
import java.util.List;

/**
 * The data structrue to store a raw example instance (early version of examples collected)
 * Created by clwang on 11/6/15.
 */
public class ExampleInstance {
    String description;
    List<Table> inputTables = new ArrayList<Table>();
    Table outputTable;

    private ExampleInstance(String description,
                           List<Table> inputTables,
                           Table outputTable) {
        this.description = description;
        this.inputTables = inputTables;
        this.outputTable = outputTable;
    }

    @Override
    public String toString() {
        String result = "";
        result += "-- Description -- \r\n" + this.description + "\r\n";
        result += "-- Input -- \r\n";
        for (Table t : this.inputTables) {
            result += t + "\r\n";
        }
        result += "-- Output --" + "\r\n";
        result += this.outputTable + "\r\n";
        return result;
    }

    public void printTables() {
        System.out.println("--- input tables ---");
        for (Table t : this.inputTables) {
            System.out.println(t + "\r\n");
        }
        System.out.println("--- output table ---");
        System.out.println(outputTable + "\r\n");
    }


    /**
     * Parse strings into an example instance
     * @param description descriptions
     * @param inputs input tables in a string
     * @param output output table source
     * @return an example instance
     */
    private static ExampleInstance parseOneExample(String description, String inputs, String output) {

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

            examples.add(parseOneExample(desc, inputE, outputE));
        }

        return examples;
    }

    // TODO: add a function to transform ExampleInstance into ExampleDS
}
