package main;

import entity.ExampleInstance;
import util.ExampleInstanceReader;
import util.IOFuction;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String filePath = "data/examples.md";
        List<String> example_file = IOFuction.readingToLines(filePath);
        List<ExampleInstance> examples = ExampleInstanceReader.readFromMarkdown(example_file);
        for (ExampleInstance eg : examples) {
            //System.out.println(eg);
            System.out.println("=== Example ===");
            eg.printTables();
        }

    }

    private static void printList(List l) {
        for (Object i : l) {
            System.out.println(i);
        }
    }
}
