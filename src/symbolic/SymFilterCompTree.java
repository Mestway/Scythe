package symbolic;

import enumerator.context.EnumContext;
import sql.lang.Table;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.filter.LogicAndFilter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.trans.ValNodeSubstBinding;
import util.CombinationGenerator;
import util.CostEstimator;
import util.Pair;
import util.RenameTNWrapper;

import java.util.*;
import java.util.stream.Collectors;

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

    public double estimateTreeCost() {
        double childCost = children.stream().map(t -> t.estimateTreeCost()).reduce(0., (x,y) -> x + y);
        return childCost + this.primitiveFilters.stream()
                .map(sf -> symTable.estimatePrimitiveSymFilterCost(sf)).reduce(0., (x,y)->x + y);
    }

    public String prettyString(int indentLevel) {

        String indent = "";
        for (int i = 0; i < indentLevel; i ++)
            indent += "\t";

        String s = "";
        s += indent + symTable.getBaseTable().getMetadata().stream().reduce("", (x,y)->(x+", "+y)).substring(2) + "\n";
        String filterString = "{";
        for (SymbolicFilter sf : this.primitiveFilters)
            filterString += sf.toString() + ", ";
        filterString = filterString.substring(0, filterString.length()-2) + "}";
        s += indent + filterString + "\n";

        for (SymFilterCompTree sfct : this.children) {
            s += sfct.prettyString(indentLevel + 1);
        }

        return s;
    }

    public List<TableNode> translateToTopSQL(EnumContext ec) {

        if (symTable instanceof SymbolicTable) {

            List<TableNode> result = new ArrayList<>();

            List<TableNode> cores = ((SymbolicTable) symTable).genTableSrc(ec);
            cores.sort(new Comparator<TableNode>() {
                @Override
                public int compare(TableNode o1, TableNode o2) {
                    return o1.estimateAllFilterCost() <= o2.estimateAllFilterCost() ?
                            (o1.estimateAllFilterCost() < o2.estimateAllFilterCost() ? -1 : 0) : 1;
                }
            });

            // select and print only the best core
            System.out.println("[(SymFitlterCompTree) Core count]: " + cores.size());
            int upperBound = cores.size() > 1 ? 1 : cores.size();
            List<TableNode> topTNs = cores.subList(0, upperBound);

            for (TableNode tn : topTNs) {

                //TableNode tn = cores.get(0);
                List<List<Filter>> unRotated = new ArrayList<>();
                for (SymbolicFilter sf : this.primitiveFilters) {
                    List<Filter> decoded = ((SymbolicTable) symTable)
                            .decodePrimitiveFilter(sf, tn, ec);
                    unRotated.add(decoded);
                }

                List<List<Filter>> rotated = CombinationGenerator.rotateList(unRotated);

                rotated.sort(new Comparator<List<Filter>>() {
                    @Override
                    public int compare(List<Filter> o1, List<Filter> o2) {
                        double score1 = CostEstimator.estimateConjFilterList(o1,
                                TableNode.nameToOriginMap(tn.getSchema(), tn.originalColumnName()));
                        double score2 = CostEstimator.estimateConjFilterList(o2,
                                TableNode.nameToOriginMap(tn.getSchema(), tn.originalColumnName()));
                        return Double.compare(score1, score2);
                    }
                });

                int SELECT_TOP = 1;
                for (int r = 0; r < rotated.size(); r ++) {
                    if (r >= SELECT_TOP) break;
                    result.add(new SelectNode(tn.getSchema().stream().map(s -> new NamedVal(s)).collect(Collectors.toList()),
                            tn, LogicAndFilter.connectByAnd(rotated.get(r))));
                }

                /* List<Filter> candidateConjFilter = null;
                double minCost = 999;

                for (List<Filter> filters : rotated) {

                    double score = CostEstimator.estimateConjFilterList(filters,
                            TableNode.nameToOriginMap(tn.getSchema(), tn.originalColumnName()));

                    if (score < minCost) {
                        minCost = score;
                        candidateConjFilter = filters;
                    }
                }*/

            }
            return result;

        } else if (symTable instanceof SymbolicCompoundTable) {

            List<TableNode> result = new ArrayList<>();

            // we shall first generate the core table of the select predicate

            // the subquery list contains two lists, the first lists are queries that are generated from st1
            // the second list contains queries generated from st2.
            List<List<TableNode>> subQueryLists = new ArrayList<>();
            for (SymFilterCompTree sfct : this.children) {
                subQueryLists.add(sfct.translateToTopSQL(ec));
            }

            List<List<TableNode>> rotated = CombinationGenerator.rotateList(subQueryLists);

            for (List<TableNode> subQueries : rotated) {

                JoinNode jn = new JoinNode(subQueries);
                RenameTableNode coreTableNode = (RenameTableNode) RenameTNWrapper.tryRename(jn);

                // Then we concrete the LR filter on this table

                Table st1Table = ((SymbolicCompoundTable) symTable).st1.getBaseTable();
                Table st2Table = ((SymbolicCompoundTable) symTable).st2.getBaseTable();
                JoinNode fakeJN = new JoinNode(Arrays.asList(new NamedTable(st1Table), new NamedTable(st2Table)));
                RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(fakeJN);

                List<Filter> filters = new ArrayList<>();

                for (SymbolicFilter sf : this.primitiveFilters) {
                    double minCost = 999; Filter candidate = null;
                    for (Filter f : ((SymbolicCompoundTable) symTable).decodeLR(sf)) {
                        double cost = CostEstimator.estimateFilterCost(f, TableNode.nameToOriginMap(rt.getSchema(), rt.originalColumnName()));
                        if (cost < minCost) {
                            candidate = f;
                            minCost = cost;
                        }
                    }
                    filters.add(candidate);
                }

                // Since the filters are built upon fakeRT, we cannot directly use these filters to construct the desired output,
                // and we will first need to rename the columns in each filter to construct a real filter

                // rename the filters so that the filters refer to the elements in the new table.
                ValNodeSubstBinding vsb = new ValNodeSubstBinding();
                for (int i = 0; i < coreTableNode.getSchema().size(); i ++) {
                    vsb.addBinding(new Pair<>(
                            new NamedVal(((SymbolicCompoundTable) symTable).representitiveTableNode.getSchema().get(i)),
                            new NamedVal(coreTableNode.getSchema().get(i))));
                }

                List<Filter> renamedFilters = new ArrayList<>();
                for (Filter f : filters) {
                    renamedFilters.add(f.substNamedVal(vsb));
                }

                result.add(new SelectNode(
                        coreTableNode.getSchema().stream().map(s -> new NamedVal(s)).collect(Collectors.toList()),
                        coreTableNode,
                        LogicAndFilter.connectByAnd(renamedFilters)));
            }
            return result;
        }
        return null;
    }

}
