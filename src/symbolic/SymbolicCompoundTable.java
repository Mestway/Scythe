package symbolic;

import enumerator.context.EnumContext;
import enumerator.primitive.EnumCanonicalFilters;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import util.CombinationGenerator;
import util.DebugHelper;
import util.Pair;
import util.RenameTNWrapper;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/26/16.
 */
public class SymbolicCompoundTable extends AbstractSymbolicTable {

    public AbstractSymbolicTable st1, st2;
    // the set of filters that use both st1 elements and st2 elements,
    // original filters on st1 and st2 are not contained here
    Set<SymbolicFilter> filtersLR = new HashSet<>();

    private boolean primitiveFiltersEvaluated = false;

    private SymbolicCompoundTable() {}

    public SymbolicCompoundTable(AbstractSymbolicTable st1,  AbstractSymbolicTable st2) {
        this.st1 = st1;
        this.st2 = st2;
        this.filtersLR = new HashSet<>();
    }

    @Override
    public Table getBaseTable() {
        return Table.joinTwo(st1.getBaseTable(), st2.getBaseTable());
    }

    @Override
    public Pair<Set<SymbolicFilter>, FilterLinks> instantiateAllFilters() {

        // make sure that primitive filters are already evaluated
        assert primitiveFiltersEvaluated;

        Set<SymbolicFilter> instantiatedFilters = new HashSet<>();

        Pair<Set<SymbolicFilter>, FilterLinks> st1FiltersLinks = st1.instantiateAllFilters();
        Pair<Set<SymbolicFilter>, FilterLinks> st2FiltersLinks = st2.instantiateAllFilters();

        Pair<Set<SymbolicFilter>, FilterLinks> lrFiltersLinks =
                AbstractSymbolicTable.mergeAndLinkFilters(this, this.filtersLR.stream().collect(Collectors.toList()));

        // currently keep all filters with me, export when needed
        FilterLinks filterLinks = FilterLinks.merge(
                Arrays.asList(
                        st1FiltersLinks.getValue(),
                        st2FiltersLinks.getValue(),
                        lrFiltersLinks.getValue()));

        for (SymbolicFilter f1 : st1FiltersLinks.getKey()) {
            for (SymbolicFilter f2 : st2FiltersLinks.getKey()) {
                for (SymbolicFilter lrf : lrFiltersLinks.getKey()) {

                    SymbolicFilter mergedFilter = SymbolicFilter.mergeFilter(
                            this.promoteLeftFilter(f1),
                            SymbolicFilter.mergeFilter(
                                    this.promoteRightFilter(f2),
                                    lrf,
                                    AbstractSymbolicTable.mergeFunction),
                            AbstractSymbolicTable.mergeFunction);

                    instantiatedFilters.add(mergedFilter);

                    Set<Pair<AbstractSymbolicTable, SymbolicFilter>> src = new HashSet<>();
                    src.add(new Pair<>(st1, f1));
                    src.add(new Pair<>(st2, f2));
                    src.add(new Pair<>(this, lrf));
                    filterLinks.addLink(src, new Pair<>(this, mergedFilter));
                }
            }
        }

        return new Pair<>(instantiatedFilters, filterLinks);
    }

    @Override
    public int getPrimitiveFilterNum() {
        return this.filtersLR.size();
    }

    @Override
    void evalPrimitive(EnumContext ec) {
        if (this.primitiveFiltersEvaluated) return;

        // first evaluate the filters for sub-abstract tables
        st1.evalPrimitive(ec);
        st2.evalPrimitive(ec);

        JoinNode jn = new JoinNode(
                Arrays.asList(
                        new NamedTable(this.st1.getBaseTable()),
                        new NamedTable(this.st2.getBaseTable())));

        RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);

        // the maximum filter length for filtersLR should be 1
        int backUpMaxFilterLength = ec.getMaxFilterLength();
        ec.setMaxFilterLength(1);
        List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterJoinNode(rt, ec);
        ec.setMaxFilterLength(backUpMaxFilterLength);

        // make sure that the empty filter is added
        filters.add(new EmptyFilter());

        for (Filter f : filters) {
            this.filtersLR.add(SymbolicFilter.genSymbolicFilterFromTableNode(rt, f));
        }

        this.primitiveFiltersEvaluated = true;
    }


    // Promote a filter in left table to a filter for current baseTable
    private SymbolicFilter promoteLeftFilter(SymbolicFilter sf1) {
        List<Integer> promotedFilter = new ArrayList<>();
        for (int i : sf1.getFilterRep()) {
            for (int j  = 0; j < this.st2.getBaseTable().getContent().size(); j ++) {
                promotedFilter.add(i * this.st2.getBaseTable().getContent().size() + j);
            }
        }
        return new SymbolicFilter(promotedFilter, this.getBaseTable().getContent().size());
    }

    // Promote a filter in right table to a filter for current baseTable
    private SymbolicFilter promoteRightFilter(SymbolicFilter sf2) {
        List<Integer> promotedFilter = new ArrayList<>();
        for (int i = 0; i < st1.getBaseTable().getContent().size(); i ++) {
            for (int j : sf2.getFilterRep()) {
                promotedFilter.add(i * this.st2.getBaseTable().getContent().size() + j);
            }
        }
        return new SymbolicFilter(promotedFilter, this.getBaseTable().getContent().size());
    }


    public List<Filter> decodeLR(Pair<AbstractSymbolicTable, SymbolicFilter> sfp, EnumContext ec) {
        return EnumCanonicalFilters.enumCanonicalFilterNamedTable(new NamedTable(this.getBaseTable()), ec)
                .stream().filter(f -> SymbolicFilter.genSymbolicFilter(this.getBaseTable(), f).equals(sfp.getValue()))
                .collect(Collectors.toList());
    }

    public List<List<Filter>> decodeLR(Set<Pair<AbstractSymbolicTable, SymbolicFilter>> srcs, EnumContext ec) {
        List<List<Filter>> unRotatedFilters = new ArrayList<>();
        for (Pair<AbstractSymbolicTable, SymbolicFilter> p : srcs) {
            if (p.getKey().equals(this)) {
                unRotatedFilters.add(decodeLR(p, ec));
            }
        }
        return CombinationGenerator.rotateList(unRotatedFilters);
    }

    // decoding the filter representation based on the links between filters
    @Override
    public List<TableNode> decodeToQuery(Pair<AbstractSymbolicTable, SymbolicFilter> sfp, EnumContext ec, FilterLinks fl) {

        assert (sfp.getKey().equals(this));

        TableNode coreTableNode = this.queryForBaseTable(ec);

        List<TableNode> result = new ArrayList<>();

        // this means that the query can be obtained by only applying a filter to it
        List<Filter> directFilter = this.decodeLR(sfp, ec);
        for (Filter f : directFilter) {
            List<ValNode> vals = coreTableNode.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());
            // rename the filters so that the filters refer to the elements in the new table.
            ValNodeSubstBinding vsb = new ValNodeSubstBinding();
            for (int i = 0; i < coreTableNode.getSchema().size(); i ++) {
                vsb.addBinding(new Pair<>(
                        new NamedVal(getBaseTable().getMetadata().get(i)),
                        new NamedVal(coreTableNode.getSchema().get(i))));
            }
            TableNode tn = new SelectNode(vals, coreTableNode, f.substNamedVal(vsb));
            result.add(tn);
        }


        Set<Set<Pair<AbstractSymbolicTable, SymbolicFilter>>> srcSet = fl.retrieveSource(sfp);
        if (srcSet == null)
            return result;

        // the followings are the queries with several leading nodes
        for (Set<Pair<AbstractSymbolicTable, SymbolicFilter>> s : srcSet) {

            List<List<Filter>> filterList = this.decodeLR(s, ec);

            // now try to decode left and right table
            List<TableNode> decodeSt1 = new ArrayList<>(), decodeSt2 = new ArrayList<>();
            for (Pair<AbstractSymbolicTable, SymbolicFilter> p : s) {
                if (p.getKey().equals(this.st1)) {
                    decodeSt1 = st1.decodeToQuery(p, ec, fl);
                }
                if (p.getKey().equals(this.st2)) {
                    decodeSt2 = st2.decodeToQuery(p, ec, fl);
                }
            }
            List<Filter> LRFilters = filterList.stream()
                    .map(lst -> LogicAndFilter.connectByAnd(lst)).collect(Collectors.toList());

            for (TableNode tn1 : decodeSt1) {
                for (TableNode tn2 : decodeSt2) {
                    JoinNode jn = new JoinNode(Arrays.asList(tn1, tn2));
                    RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
                    for (Filter f : LRFilters) {
                        ValNodeSubstBinding vsb = new ValNodeSubstBinding();
                        for (int i = 0; i < coreTableNode.getSchema().size(); i ++) {
                            vsb.addBinding(new Pair<>(
                                    new NamedVal(getBaseTable().getMetadata().get(i)),
                                    new NamedVal(rt.getSchema().get(i))));
                        }
                        List<ValNode> vals = rt.getSchema().stream()
                                .map(x -> new NamedVal(x))
                                .collect(Collectors.toList());
                        TableNode tn = new SelectNode(vals, rt, f.substNamedVal(vsb));
                        result.add(tn);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public TableNode queryForBaseTable(EnumContext ec) {
        return RenameTNWrapper.tryRename(new JoinNode(Arrays.asList(st1.queryForBaseTable(ec), st2.queryForBaseTable(ec))));
    }
}
