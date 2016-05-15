package symbolic;

import enumerator.context.EnumContext;
import enumerator.tableenumerator.SymbolicTableEnumerator;
import global.Statistics;
import sql.lang.Table;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.TableNode;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import util.CombinationGenerator;
import util.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 */
public abstract class AbstractSymbolicTable {

    public int primitiveBitVecFilterCount = 0;
    public int primitiveSynFilterCount = 0;

    public boolean allfiltersEnumerated = false;
    public int totalBitVecFiltersCount = 0;

    // this determines that the method "combining filters" will only generate filters of length 2
    static int maxFilterLength = 2;
    static public final BiFunction<Boolean, Boolean, Boolean> mergeFunction = (x, y) -> x && y;

    abstract public Table getBaseTable();
    abstract public Pair<Set<SymbolicFilter>, FilterLinks> instantiateAllFilters();
    abstract public int getPrimitiveFilterNum();
    abstract public int compoundPrimitiveFilterCount();
    abstract public int compoundFilterCount();
    abstract public Pair<Set<SymbolicFilter>, FilterLinks> lastStageInstantiateAllFilters(Set<SymbolicFilter> targetFilters);

    // the lazy function that only calculate the primitive filters when necessary,
    abstract void evalPrimitive(EnumContext ec);

    public void emitInstantiateAllTables(
            EnumContext ec,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {

        this.evalPrimitive(ec);

        Pair<Set<SymbolicFilter>, FilterLinks> p = this.instantiateAllFilters();

        Set<SymbolicFilter> filters = p.getKey();

        for (SymbolicFilter spf : filters) {
            f.accept(new Pair<>(this, spf), p.getValue());
        }

        System.out.println("[Valid Filter count / Stored Bitvec Count] "
                + SymbolicTableEnumerator.validFilterBitVecCount + " / " + p.getKey().size()
                + " = " + SymbolicTableEnumerator.validFilterBitVecCount / ((float) p.getKey().size()));
    }

    public void lastStageEmitInstanitateAllTables(
            Set<SymbolicFilter> targetFilters,
            EnumContext ec,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {

        this.evalPrimitive(ec);
        Pair<Set<SymbolicFilter>, FilterLinks> p = this.lastStageInstantiateAllFilters(targetFilters);
        for (SymbolicFilter spf : p.getKey()) {
            f.accept(new Pair<>(this, spf), p.getValue());
        }

        Statistics.sum_back_filter_bogus_rate += ((float)(targetFilters.size() - p.getKey().size())) / targetFilters.size();
        Statistics.back_filter_bogus_cases ++;


        //System.out.println("Real vs inferred: " + p.getKey().size() + " ~ " + targetFilters.size());
    }

    // the status is returned, either the filter we are looking up is a valid or not.
    public boolean lazyConsumeTable(
            int index,
            EnumContext ec,
            BiConsumer<Pair<AbstractSymbolicTable, SymbolicFilter>, FilterLinks> f) {

        this.evalPrimitive(ec);

        Optional<Pair<SymbolicFilter, FilterLinks>> op = lazyFilterEval(index);

        if (!op.isPresent()) { return false; }

        f.accept(new Pair<>(this, op.get().getKey()), op.get().getValue());
        return true;
    }

    abstract public Optional<Pair<SymbolicFilter, FilterLinks>> lazyFilterEval(Integer index);

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

                SymbolicFilter mergedFilter = SymbolicFilter.mergeFilter(
                        primitives.get(i),
                        primitives.get(j),
                        AbstractSymbolicTable.mergeFunction);
                filters.add(mergedFilter);

                // add the link from two source filter to the merged filter itself
                Set<Pair<AbstractSymbolicTable, SymbolicFilter>> srcs = new HashSet<>();
                srcs.add(new Pair<>(ast, primitives.get(i)));
                srcs.add(new Pair<>(ast, primitives.get(j)));

                filterLinks.addLink(srcs, new Pair<>(ast, mergedFilter));
            }
        }

        // this is used to make sure that the empty filter is added
        filters.add(SymbolicFilter.genSymbolicFilter(ast.getBaseTable(), new EmptyFilter()));
        return new Pair<>(filters, filterLinks);
    }

    public abstract List<TableNode> decodeToQuery(Pair<AbstractSymbolicTable, SymbolicFilter> sfp, EnumContext ec, FilterLinks fl);
    public abstract TableNode queryForBaseTable(EnumContext ec);

    // index = sum_{k=1}^{i} (n-k) + (j-i)
    // TODO: use a formula to simplify this
    protected Pair<Integer, Integer> inverseFilterIndex(int n, int index) {
        for (int i = 0; i < n; i ++) {
            for (int j = i + 1; j < n; j ++) {
                int ind =  n * i - i * (i + 1) / 2 + (j - i) - 1;
                if (ind == index)
                    return new Pair<>(i, j);
            }
        }
        return new Pair<>(-1, -1);
    }

    // checks whether sf contains at least one filter in the target.
    protected boolean fullyContainedAnElement(
            SymbolicFilter sf, Set<SymbolicFilter> target) {
        for (SymbolicFilter f : target) {
            if (sf.fullyContained(f)) {
                return true;
            }
        }
        return false;
    }
}
