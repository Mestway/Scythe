package main;

import enumerator.tableenumerator.AbstractTableEnumerator;
import enumerator.tableenumerator.PlainTableEnumerator;
import sql.lang.ast.table.TableNode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 3/22/16.
 */
public class Synthesizer {

    public static List<TableNode> Synthesize(String path, AbstractTableEnumerator enumerator) {

        // read file
        ExampleDS exampleDS = ExampleDS.readFromFile(path);
        System.out.println("================\n[[Synthesizer start]] " + path);
        System.out.println("[Enumerator Type] " + enumerator.getClass().getSimpleName());
        long timeStart = System.currentTimeMillis();


        // synthesize
        List<TableNode> candidates = enumerator.enumProgramWithIO(exampleDS.inputs, exampleDS.output, exampleDS.enumConstraint);

        /*
        Path file = Paths.get("output//StackOverflow//" + path.substring(path.length() -3, path.length())
                + "-" + enumerator.getClass().getSimpleName());
        try {
            List<String> lines = new ArrayList<>();
            for (TableNode tn : candidates) {
                lines.add(tn.prettyPrint(0));
                lines.add(" ########################################### ");
            }
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        // formatting time
        long timeUsed = System.currentTimeMillis() - timeStart;
        long second = (timeUsed / 1000) % 60;
        long minute = (timeUsed / (1000 * 60)) % 60;
        long hour = (timeUsed / (1000 * 60 * 60)) % 24;
        String time = String.format("%02d:%02d:%02d:%d", hour, minute, second, timeUsed % 1000);

        System.out.println("[[Synthesizer finished]] time: " + time);
        return candidates;
    }
}
