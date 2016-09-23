package main;

import forward_enumeration.table_enumerator.AbstractTableEnumerator;
import forward_enumeration.table_enumerator.CanonicalTableEnumerator;
import forward_enumeration.table_enumerator.PlainTableEnumerator;
import org.junit.Test;
import scythe_interface.Synthesizer;
import sql.lang.ast.table.TableNode;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by clwang on 3/22/16.
 */
public class SynthesizerTest {

    @Test
    public void synthesize() throws Exception {

        List<AbstractTableEnumerator> enumerators = new ArrayList<>();
        enumerators.add(new PlainTableEnumerator());
        enumerators.add(new CanonicalTableEnumerator());

        // testing with time limit
        File dir = new File("data//StackOverflow");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File f : directoryListing) {
                if (f.isFile()) {
                    if (f.getName().endsWith("020")) continue;
                    System.out.println("\n[[~~~~~~~~]]");
                    for (AbstractTableEnumerator enumerator : enumerators) {
                        //Synthesizer.Synthesize(f.getPath(), enumerator);

                        final Duration timeout = Duration.ofMinutes(10);
                        ExecutorService executor = Executors.newSingleThreadExecutor();

                        final Future<List<TableNode>> handler = executor.submit(new Callable<List<TableNode>>() {
                            @Override
                            public List<TableNode> call() throws Exception {
                                return Synthesizer.Synthesize(f.getPath(), 2, enumerator);
                            }
                        });

                        try {
                            handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                        } catch (TimeoutException e) {
                            handler.cancel(true);
                        }

                        executor.shutdownNow();
                    }
                }
            }
        }
    }

}