package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 10/27/15.
 */
public class IOFuction {

    public static List<String> readingToLines(String inputPath) {
        List<String> list = new ArrayList<String>();

        Path path = Paths.get(inputPath);
        try {
            Files.lines(path).forEachOrdered(s -> list.add(s));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }
}
