package enumerator.context;

import sql.lang.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The data structure used to store how a table can be generated from other tables.
 *          node
 *          /   \
 *       node1  node2
 * indicates that the 'node' can be generated from node1 and node 2
 * Each table tree corresponds to a bunch of SQL queries to generate the root node table.
 * Created by clwang on 4/7/16.
 */
public class TableTreeNode {
    Table node;
    List<TableTreeNode> children = new ArrayList<>();

    public TableTreeNode(Table node, List<TableTreeNode> children) {
        this.node = node;
        this.children = children;
    }

    public Table getTable() {
        return this.node;
    }

    public List<TableTreeNode> getChildren() { return this.children; }

    // test if all of the leaf nodes of the tree are valid.
    public boolean leafValid(Set<Table> validLeaves) {
        if (this.children.size() == 0) {
            return validLeaves.contains(this.node);
        } else {
            boolean valid = true;
            for (TableTreeNode ttn : children) {
                valid = valid && ttn.leafValid(validLeaves);
            }
            return valid;
        }
    }

    public void print(int depth) {
        String indent = "";
        for (int i = 0; i < depth; i ++) {
            indent += "    ";
        }

        System.out.println(this.node.toStringWithIndent(indent));
        for (TableTreeNode ttn : this.children) {
            ttn.print(depth + 1);
        }
    }

}
