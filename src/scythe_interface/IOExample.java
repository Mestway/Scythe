package scythe_interface;

import forward_enumeration.context.EnumConfig;
import lang.table.Table;
import util.TableExampleParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The data structure used to store an example, with concrete enumeration context
 */
public class IOExample {
    public List<Table> inputs = new ArrayList<>();
    public Table output;
    public EnumConfig enumConfig;

    private IOExample() {}

    public static IOExample readFromFile(String path) {

        IOExample example = new IOExample();

        List<String> fileContent;
        try {
            fileContent = Files.readAllLines(Paths.get(path))
                    .stream().filter(t -> !t.startsWith("//")).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Set<String> usedInputTableNames = new HashSet<>();
        Map<String, List<String>> segments = new HashMap<>();

        // segmenting the IOExample based on splitters
        int i = 0;
        while (i < fileContent.size()) {
            if (fileContent.get(i).startsWith("#")) {
                String segName = fileContent.get(i).substring(1).trim();
                i ++;
                List<String> segContent = new ArrayList<>();
                while (i < fileContent.size() && !fileContent.get(i).startsWith("#")) {
                    if (!fileContent.get(i).trim().isEmpty())
                        segContent.add(fileContent.get(i));
                    i ++;
                }
                segments.put(segName, segContent);
            } else {
                i ++;
            }
        }

        for (Map.Entry<String, List<String>> entry : segments.entrySet()) {

            String segName = entry.getKey();
            List<String> segContent = entry.getValue();

            if (segName.startsWith("input")) {

                String baseTableName = segName.substring("input".length());
                if (baseTableName.equals(""))
                    baseTableName = "input";
                else
                    baseTableName = baseTableName.substring(1);

                String tableName = baseTableName;
                int id = 0;
                while (usedInputTableNames.contains(tableName)) {
                    tableName = baseTableName + id;
                    id++;
                }
                usedInputTableNames.add(tableName);

                example.inputs.add(TableExampleParser.tryParseTable(tableName, segContent));
            } else if (segName.startsWith("output")) {
                example.output = TableExampleParser.tryParseTable("output", segContent);
            } else if (segName.startsWith("constraint")) {
                example.enumConfig = new EnumConfig(segContent.stream().reduce(String::concat).get());
            } // add other segments here
        }
        return example;
    }

    @Override
    public String toString() {
        String s = "";
        for (Table t : inputs) {
            s += "----\n" + t.toString() + "\n";
        }
        s += "----\n" + output.toString() + "\n";
        s += enumConfig.toString();
        return s;
    }

}