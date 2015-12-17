package entity;

import sql.lang.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 11/6/15.
 */
public class ExampleInstance {
    String description;
    List<Table> inputTables = new ArrayList<Table>();
    Table outputTable;

    public ExampleInstance(String description,
                           List<Table> inputTables,
                           Table outputTable) {
        this.description = description;
        this.inputTables = inputTables;
        this.outputTable = outputTable;
    }

    @Override
    public String toString() {
        String result = "";
        result += "-- Description -- \r\n" + this.description + "\r\n";
        result += "-- Input -- \r\n";
        for (Table t : this.inputTables) {
            result += t + "\r\n";
        }
        result += "-- Output --" + "\r\n";
        result += this.outputTable + "\r\n";
        return result;
    }

    public void printTables() {
        System.out.println("--- input tables ---");
        for (Table t : this.inputTables) {
            System.out.println(t + "\r\n");
        }
        System.out.println("--- output table ---");
        System.out.println(outputTable + "\r\n");
    }

}
