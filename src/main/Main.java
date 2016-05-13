package main;

import enumerator.tableenumerator.AbstractTableEnumerator;
import enumerator.tableenumerator.CanonicalTableEnumeratorOnTheFly;
import enumerator.tableenumerator.CanonicalWithoutExistsEnumerator;
import enumerator.tableenumerator.SymbolicTableEnumerator;
import global.Statistics;

public class Main {

    // the interface for running the tool in
    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("Not enough arguments provided.");
            System.exit(-1);
        }

        String filename = args[0];
        String enumerator = args[1];
        int maxDepth = Integer.parseInt(args[2]);
        Synthesizer.Synthesize(filename, maxDepth, enumeratorSwitch(enumerator));

        Statistics.printAllStatistics();
    }

    public static AbstractTableEnumerator enumeratorSwitch(String name) {
        if (name.equals("SymbolicEnumerator"))
            return new SymbolicTableEnumerator();
        else if (name.equals("CanonicalEnumeratorOnTheFly"))
            return new CanonicalTableEnumeratorOnTheFly();
        else {
            System.out.println("The enumerator ["+ name + "] is currently not supported.");
            System.exit(-1);
        }
        return null;
    }

}
