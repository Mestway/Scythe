package util;

import sql.lang.ast.table.RenameTableNode;
import sql.lang.ast.table.TableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 1/7/16.
 */
public class RenameTNWrapper {

    static int renamingIndex = 0;

    /*****************************************************
     Renaming enumerated tables
     used for renaming anonymous table that will be
     used later enumeration process
     ****************************************************/

    public static TableNode tryRename(TableNode tn) {
        if (tn.getTableName().equals("anonymous")) {
            String newName = "defaultTable_" + renamingIndex;
            renamingIndex ++;
            List<String> newSchema = new ArrayList<String>();
            for (String s : tn.getSchema()) {
                String shortName = s.substring(s.lastIndexOf(".") + 1);
                if (newSchema.contains(shortName)) {
                    int i = 1;
                    while(newSchema.contains(shortName + i)) {
                        i ++;
                    }
                    newSchema.add(shortName + i);
                } else {
                    newSchema.add(shortName);
                }
            }
            return new RenameTableNode(newName, newSchema, tn);
        } else
            return tn;
    }
}
