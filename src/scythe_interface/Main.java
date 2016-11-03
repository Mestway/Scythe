package scythe_interface;

import forward_enumeration.table_enumerator.*;
import global.Statistics;

public class Main {

    // the interface for running the tool in
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("[ERROR] Not enough arguments provided.");
            System.out.println("  usage: java -jar path enumerator_name");
            System.exit(-1);
        }

        // for logging purpose
        System.setErr(System.out);

        String filename = args[0];
        String enumerator = args[1];
        SynthesizerWAggrfun.Synthesize(filename, enumeratorSwitch(enumerator));

        // Statistics.printAllStatistics();
    }

    public static AbstractTableEnumerator enumeratorSwitch(String name) {
        if (name.equals("StagedEnumerator"))
            return new StagedEnumerator();
        else if (name.equals("CanonicalEnumeratorOnTheFly"))
            return new CanonicalTableEnumeratorOnTheFly();
        else {
            System.out.println("The enumerator [" + name + "] is currently not supported.");
            System.exit(-1);
        }
        return null;
    }

}
