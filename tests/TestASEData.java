import enumerator.Constraint;
import enumerator.hueristics.HeuristicTableJoin;
import org.junit.Test;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.TableNode;
import synthesis.Synthesizer;
import util.IOFuction;
import util.TableInstanceParser;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 2/20/16.
 */
public class TestASEData {


    @Test
    public void testAll() throws Exception {
        testOneInstance("data//ase13-data//dat//5_1_1");
    }

    public TableNode testOneInstance(String dirPath) throws Exception {
        File dir = new File(dirPath);
        System.out.print(dir.getAbsolutePath());
        String constraint = "";

        List<Table> inputs = new ArrayList<>();
        Table output = null;
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {

                System.out.print(f.getName());
                String fileName = f.getName();
                if (fileName.equals("constraint")) {
                    constraint = IOFuction.readingToLines(f.toPath().toString()).stream().reduce("", (x, y) -> x + " " + y);
                    continue;
                }
                Table table = parseOneTable(f.toPath().toString(), fileName);
                System.out.println(table);
                if (fileName.equals("output"))
                    output = table;
                else
                    inputs.add(table);
            }
        }

        TableNode tn = HeuristicTableJoin.equiJoin(inputs);
        System.out.println(tn.prettyPrint(0));
        System.out.println(tn.eval(new Environment()));
        System.out.println(constraint);

        new Synthesizer().synthesize(inputs, output, new Constraint(2,
                new ArrayList<>(),
                Arrays.asList(AggregationNode.AggrMax, AggregationNode.AggrCount),
                0));

        return null;
    }

    public Table parseOneTable(String path, String tableName) throws Exception {
        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
        List<String> lines = IOFuction.readingToLines(path);
        System.out.println();
        return TableInstanceParser.parseCVS(tableName, lines);
    }

}
