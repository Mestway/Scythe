package symbolic;

import enumerator.context.EnumContext;
import sql.lang.Table;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.table.TableNode;
import util.CombinationGenerator;
import util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 */
public abstract class AbstractSymbolicTable {

    // this determines that the method "combining filters" will only generate filters of length 2
    static int maxFilterLength = 2;
    static public final BiFunction<Boolean, Boolean, Boolean> mergeFunction = (x, y) -> x && y;

    abstract public Table getBaseTable();
    abstract public Pair<Set<SymbolicFilter>, FilterLinks> instantiateAllFilters();
    abstract public int getPrimitiveFilterNum();

    // the lazy function that only calculate the primitive filters when necessary,
    abstract void evalPrimitive(EnumContext ec);

    public void emitInstantiateAllTables(
            EnumContext ec,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {

        this.evalPrimitive(ec);

        Pair<Set<SymbolicFilter>, FilterLinks> p = this.instantiateAllFilters();

        Set<SymbolicFilter> filters = p.getKey();
        Table table = this.getBaseTable();

        for (SymbolicFilter spf : filters) {
            /* Table t = table.duplicate();
            t.getContent().clear();
            for (Integer i : spf.getFilterRep()) {
                t.getContent().add(table.getContent().get(i).duplicate());
            } */
            f.accept(new Pair<>(this, spf), p.getValue());
        }
    }

    public List<Table> instantiateAllTables(EnumContext ec) {

        this.evalPrimitive(ec);

        Set<SymbolicFilter> filters = this.instantiateAllFilters().getKey();
        Table table = this.getBaseTable();

        List<Table> instantiatedTables = new ArrayList<>();
        for (SymbolicFilter spf : filters) {
            Table t = table.duplicate();
            t.getContent().clear();
            for (Integer i : spf.getFilterRep()) {
                t.getContent().add(table.getContent().get(i).duplicate());
            }
            instantiatedTables.add(t);
        }
        return instantiatedTables.stream().filter(t -> t.getContent().size() > 0).collect(Collectors.toList());
    }

    // TODO: currently only allow filters of length 2
    // The first
    public static Pair<Set<SymbolicFilter>, FilterLinks> mergeAndLinkFilters(
            AbstractSymbolicTable ast,
            List<SymbolicFilter> primitives) {

        FilterLinks filterLinks = new FilterLinks();
        Set<SymbolicFilter> filters = new HashSet<>();
        for (int i = 0; i < primitives.size(); i ++) {
            for (int j = i + 1; j < primitives.size(); j ++) {

                SymbolicFilter mergedQuery = SymbolicFilter.mergeFilter(
                        primitives.get(i),
                        primitives.get(j),
                        AbstractSymbolicTable.mergeFunction);
                filters.add(mergedQuery);

                // add the link from two source filter to the merged filter itself
                Set<Pair<AbstractSymbolicTable, SymbolicFilter>> srcs = new HashSet<>();
                srcs.add(new Pair<>(ast, primitives.get(i)));
                srcs.add(new Pair<>(ast, primitives.get(j)));

                filterLinks.addLink(srcs, new Pair<>(ast, mergedQuery));
            }
        }

        // this is used to make sure that the empty filter is added
        filters.add(SymbolicFilter.genSymbolicFilter(ast.getBaseTable(), new EmptyFilter()));
        return new Pair<>(filters, filterLinks);
    }

    public abstract List<TableNode> decodeToQuery(Pair<AbstractSymbolicTable, SymbolicFilter> sfp, EnumContext ec, FilterLinks fl);
    public abstract TableNode queryForBaseTable(EnumContext ec);
}
