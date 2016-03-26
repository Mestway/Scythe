package main;

import entity.ExampleDS;
import org.junit.Test;

import java.io.File;

/**
 * Created by clwang on 3/22/16.
 */
public class ExampleDSTest {

    @Test
    public void readFromFile() throws Exception {

        File dir = new File("data//StackOverflow");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                System.out.println("=== " + child.getName());
                System.out.println(ExampleDS.readFromFile(child.getPath()));
            }
        } else {
            // Handle the case where dir is not really a directory.
            // Checking dir.isDirectory() above would not be sufficient
            // to avoid race conditions with another process that deletes
            // directories.
        }

    }
}