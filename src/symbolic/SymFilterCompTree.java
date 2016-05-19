package symbolic;

import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by clwang on 5/19/16.
 */
public class SymFilterCompTree {

    // the symtable for this node
    AbstractSymbolicTable symTable;

    // all primitive filters at this level used to generate the outer most level
    // each symbolic filter here is a primitive filter at the level of symTable,
    // i.e. for SymbolicCompoundTable, it is a LR filter
    //      for SymbolicTable, it is a primitive select predicate
    Set<SymbolicFilter> primitiveFilters;

    // child comp tree nodes, sub-symbolic tables that composed to this sym table
    List<SymFilterCompTree> children = new ArrayList<>();

    public SymFilterCompTree(AbstractSymbolicTable st, Set<SymbolicFilter> primitiveFilters) {
        this.symTable = st;
        this.primitiveFilters = primitiveFilters;
    }

    public void addChildren(SymFilterCompTree sct) {
        this.children.add(sct);
    }

    public List<SymFilterCompTree> getChildren() { return this.children; }
    public AbstractSymbolicTable getSymTable() { return this.symTable; }
    public Set<SymbolicFilter> getPrimitiveFilters() { return this.primitiveFilters; }

}
