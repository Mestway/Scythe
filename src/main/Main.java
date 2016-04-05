package main;

import entity.ExampleDS;
import enumerator.tableenumerator.AbstractTableEnumerator;
import enumerator.tableenumerator.CanonicalTableEnumeratorOnTheFly;
import enumerator.tableenumerator.PlainTableEnumerator;
import enumerator.tableenumerator.SymbolicTableEnumerator;

import java.io.File;

public class Main {

    // the interface for running the tool in
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Not enough arguments provided.");
            System.exit(-1);
        }
        String filename = args[0];
        String enumerator = args[1];
        Synthesizer.Synthesize(filename, enumeratorSwitch(enumerator));
    }

    public static AbstractTableEnumerator enumeratorSwitch(String name) {
        if (name.equals("SymbolicTableEnumerator"))
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
