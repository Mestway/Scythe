package main;

import entity.ExampleDS;
import enumerator.tableenumerator.PlainTableEnumerator;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        File dir = new File("data//StackOverflow");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File f : directoryListing) {
                if (f.isFile()) {
                    ExampleDS example = ExampleDS.readFromFile(f.getPath());
                    new PlainTableEnumerator().enumProgramWithIO(example.inputs, example.output, example.enumConstraint);
                }
            }
        }
    }

}
