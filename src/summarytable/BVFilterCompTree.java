package summarytable;

import forward_enumeration.context.EnumContext;
import global.Statistics;
import lang.table.Table;
import lang.sql.ast.predicate.EmptyPred;
import lang.sql.ast.predicate.Predicate;
import lang.sql.ast.predicate.LogicAndPred;
import lang.sql.ast.predicate.LogicOrPred;
import lang.sql.ast.contable.*;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.ValNode;
import lang.sql.transformation.ValNodeSubstitution;
import util.CombinationGenerator;
import util.CostEstimator;
import util.Pair;
import util.RenameWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 5/19/16.
 * The class representing how a BVFilter can be composed with different filters
 */
public class BVFilterCompTree {

    // the symtable for this node
    AbstractSummaryTable symTable;

    // all primitive filters at this level used to generate the outer most level
    // each symbolic eval here is a primitive eval at the level of symTable,
    // i.e. for SymbolicCompoundTable, it is a LR eval
    //      for SymbolicTable, it is a primitive select predicate
    Set<BVFilter> primitiveFilters;

    // child comp tree nodes, sub-symbolic tables that composed to this sym table
    List<BVFilterCompTree> children = new ArrayList<>();

    // this flag indicates whether primitive filters in this node should be connected by AND or OR.
    boolean disjComposition = false;

    public BVFilterCompTree(AbstractSummaryTable st, Set<BVFilter> primitiveFilters) {
        this(st, primitiveFilters, false);
    }

    public BVFilterCompTree(AbstractSummaryTable st, Set<BVFilter> primitiveFilters, boolean disjComposition) {
        this.symTable = st;
        this.primitiveFilters = primitiveFilters;
        this.disjComposition = disjComposition;
    }

    public void addChildren(BVFilterCompTree sct) {
        this.children.add(sct);
    }

    public List<BVFilterCompTree> getChildren() { return this.children; }
    public AbstractSummaryTable getSymTable() { return this.symTable; }
    public Set<BVFilter> getPrimitiveFilters() { return this.primitiveFilters; }

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
        s += indent + symTable.getBaseTable().getSchema().stream()
                .reduce("", (x,y)->(x+", "+y)).substring(2) + "\n";
        String filterString = "{";
        for (BVFilter sf : this.primitiveFilters)
            filterString += sf.toString() + ", ";
        filterString = filterString.substring(0, filterString.length()-2) + "}";
        s += indent + filterString + "\n";

        for (BVFilterCompTree sfct : this.children) {
            s += sfct.prettyString(indentLevel + 1);
        }

        return s;
    }

    public List<TableNode> translateToTopSQL(EnumContext ec) {

        if (symTable instanceof PrimitiveSummaryTable) {

            List<TableNode> result = new ArrayList<>();

            List<TableNode> cores = ((PrimitiveSummaryTable) symTable).genTableSrc(ec);
            // sort decodings of the core
            cores.sort((tn1, tn2)-> Double.compare(tn1.estimateAllFilterCost(), tn2.estimateAllFilterCost()));

            // select and print only the best core
            // System.out.println("[(SymFitlterCompTree) Core count]: " + cores.size());
            int upperBound = cores.size() > 1 ? 1 : cores.size();
            List<TableNode> topTNs = cores.subList(0, upperBound);

            for (TableNode tn : topTNs) {

                //TableNode tn = cores.get(0);
                List<List<Predicate>> unRotated = new ArrayList<>();
                for (BVFilter sf : this.primitiveFilters) {
                    List<Predicate> decoded = ((PrimitiveSummaryTable) symTable)
                            .decodePrimitiveFilter(sf, tn);

                    decoded.sort(new Comparator<Predicate>() {
                        @Override
                        public int compare(Predicate o1, Predicate o2) {
                            double score1 = CostEstimator.estimateFilterCost(o1,
                                    TableNode.nameToOriginMap(tn.getSchema(), tn.originalColumnName()));
                            double score2 = CostEstimator.estimateFilterCost(o2,
                                    TableNode.nameToOriginMap(tn.getSchema(), tn.originalColumnName()));
                            return Double.compare(score1, score2);
                        }
                    });

                    unRotated.add(decoded.subList(0,decoded.size() > 5? 5 : decoded.size()));
                }

                List<List<Predicate>> rotated = CombinationGenerator.rotateList(unRotated);

                // sort all combinations and choose the best one
                /*rotated.sort(new Comparator<List<Filter>>() {
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
                }*/

                 List<Predicate> candidateConjFilter = null;
                double minCost = 999;

                for (List<Predicate> filters : rotated) {

                    double score = CostEstimator.estimateConjFilterList(filters,
                            TableNode.nameToOriginMap(tn.getSchema(), tn.originalColumnName()));

                    if (score < minCost) {
                        minCost = score;
                        candidateConjFilter = filters;
                    }
                }

                List<ValNode> newSchema = tn.getSchema().stream().map(s -> new NamedVal(s)).collect(Collectors.toList());
                if (this.disjComposition == false) {
                    result.add(new SelectNode(newSchema, tn, LogicAndPred.connectByAnd(candidateConjFilter)));
                } else {
                    result.add(new SelectNode(newSchema, tn, LogicOrPred.connectByOr(candidateConjFilter)));
                }
            }
            return result;

        } else if (symTable instanceof CompoundSummaryTable) {

            List<TableNode> result = new ArrayList<>();

            // we shall first generate the core table of the select predicate

            // the subquery list contains two lists, the first lists are queries that are generated from st1
            // the second list contains queries generated from st2.
            List<List<TableNode>> subQueryLists = new ArrayList<>();
            for (BVFilterCompTree sfct : this.children) {
                subQueryLists.add(sfct.translateToTopSQL(ec));
            }

            List<List<TableNode>> rotated = CombinationGenerator.rotateList(subQueryLists);
            if (((CompoundSummaryTable) symTable).compositionType.equals(CompoundSummaryTable.CompositionType.union)) {
                for (List<TableNode> subQueries : rotated) {
                    UnionNode un = new UnionNode(subQueries);
                    RenameTableNode coreTableNode = (RenameTableNode) RenameWrapper.tryRename(un);

                    result.add(new SelectNode(
                            coreTableNode.getSchema().stream().map(s -> new NamedVal(s)).collect(Collectors.toList()),
                            coreTableNode, new EmptyPred()));
                }
                return result;

            } else if (((CompoundSummaryTable) symTable).compositionType
                    .equals(CompoundSummaryTable.CompositionType.join)) {

                for (List<TableNode> subQueries : rotated) {

                    List<String> usedTableNames = new ArrayList<>();
                    List<TableNode> renamedSubQueries = new ArrayList<>();
                    for (TableNode tn : subQueries) {
                        if (usedTableNames.contains(tn.getTableName())) {
                            TableNode rt = RenameWrapper.tryRename(tn);
                            usedTableNames.add(rt.getTableName());
                            renamedSubQueries.add(rt);
                        } else {
                            usedTableNames.add(tn.getTableName());
                            renamedSubQueries.add(tn);
                        }
                    }

                    JoinNode jn = new JoinNode(renamedSubQueries);
                    RenameTableNode coreTableNode = (RenameTableNode) RenameWrapper.tryRename(jn);

                    // Then we concrete the LR eval on this table

                    Table st1Table = ((CompoundSummaryTable) symTable).st1.getBaseTable();
                    Table st2Table = ((CompoundSummaryTable) symTable).st2.getBaseTable();
                    JoinNode fakeJN = new JoinNode(Arrays.asList(new NamedTableNode(st1Table), new NamedTableNode(st2Table)));
                    RenameTableNode rt = (RenameTableNode) RenameWrapper.tryRename(fakeJN);

                    List<Predicate> filters = new ArrayList<>();

                    for (BVFilter sf : this.primitiveFilters) {
                        double minCost = 999; Predicate candidate = null;

                        Statistics.sumLRFilterCount += ((CompoundSummaryTable) symTable).decodeLR(sf).size();
                        Statistics.cntLRFilterCount ++;
                        Statistics.maxLRFilterCount = Statistics.maxLRFilterCount
                                > ((CompoundSummaryTable) symTable).decodeLR(sf).size() ?
                                Statistics.maxLRFilterCount : ((CompoundSummaryTable) symTable).decodeLR(sf).size();

                        for (Predicate f : ((CompoundSummaryTable) symTable).decodeLR(sf)) {
                            double cost = CostEstimator.estimateFilterCost(f,
                                    TableNode.nameToOriginMap(rt.getSchema(), rt.originalColumnName()));
                            if (cost < minCost) {
                                candidate = f;
                                minCost = cost;

                            }
                            // TODO: This one does not limit the number since it adds all filters into the result set
                            //filters.add(f);
                        }
                        // TODO: if we want to limit the number, use this one
                        filters.add(candidate);
                    }

                    // Since the filters are built upon fakeRT,
                    // we cannot directly use these filters to construct the desired output,
                    // and we will first need to rename the columns in each eval to construct a real eval

                    // rename the filters so that the filters refer to the elements in the new table.
                    ValNodeSubstitution vsb = new ValNodeSubstitution();
                    for (int i = 0; i < coreTableNode.getSchema().size(); i ++) {
                        vsb.addBinding(new Pair<>(
                                new NamedVal(((CompoundSummaryTable) symTable).representativeTableNode.getSchema().get(i)),
                                new NamedVal(coreTableNode.getSchema().get(i))));
                    }

                    List<Predicate> renamedFilters = new ArrayList<>();
                    for (Predicate f : filters) {
                        renamedFilters.add(f.substNamedVal(vsb));
                    }

                    result.add(new SelectNode(
                            coreTableNode.getSchema().stream().map(s -> new NamedVal(s)).collect(Collectors.toList()),
                            coreTableNode,
                            LogicAndPred.connectByAnd(renamedFilters)));
                }
                return result;
            }
        }
        return null;
    }

}
