package sql.lang.ast.table;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.val.ValType;
import sql.lang.val.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.transformation.ValNodeSubstitution;
import util.IndentationManager;
import util.Pair;
import util.RenameWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/18/15.
 * Join is implemented as cartesian product
 */
public class LeftJoinNode extends TableNode {

    TableNode tn1, tn2;
    List<Pair<String, String>> joinKeys;

    public LeftJoinNode(TableNode tn1, TableNode tn2, List<Pair<String, String>> joinKeys) {
        this.tn1 = tn1;
        this.tn2 = tn2;
        this.joinKeys = joinKeys;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        Table t1 = tn1.eval(env);
        Table t2 = tn2.eval(env);

        List<Pair<Integer, Integer>> joinKeyIndexes = joinKeys
                .stream()
                .map(p -> new Pair<>(t1.retrieveIndex(p.getKey()), t2.retrieveIndex(p.getValue())))
                .collect(Collectors.toList());

        int bias = t1.getContent().get(0).getValues().size();

        Table cartesianProd = Table.joinTwo(t1, t2);
        Table tempTable = cartesianProd.duplicate();
        tempTable.getContent().clear();
        for (TableRow r : cartesianProd.getContent()) {
            boolean joinCond = true;
            for (Pair<Integer, Integer> p : joinKeyIndexes) {
                if (! r.getValue(p.getKey()).equals(r.getValue(bias + p.getValue())))
                    joinCond = false;
            }
            if (joinCond == true)
                tempTable.getContent().add(r);
        }

        List<Integer> projCols = new ArrayList<>();
        for (int i = 0; i < bias; i ++) {
            projCols.add(i);
        }
        List<Pair<String, ValType>> pl = new ArrayList<>();
        for (int i = 0; i < t2.getSchema().size(); i ++) {
            pl.add(new Pair<>(t2.getSchema().get(i), t2.getSchemaType().get(i)));
        }

        Table t = Table.except(t1, tempTable.projection(projCols));
        Table nullExtension = Table.extendWithNull(t, pl);

        Table unionResult = null;
        unionResult = Table.union(tempTable, nullExtension);

        return unionResult;
    }

    @Override
    public List<String> getSchema() {
        List<String> schema = new ArrayList<String>();
        schema.addAll(tn1.getSchema());
        schema.addAll(tn2.getSchema());
        return schema;
    }

    @Override
    public String getTableName() {
        return "anonymous";
    }

    @Override
    public List<ValType> getSchemaType() {
        List<ValType> valTypes = new ArrayList<ValType>();
        valTypes.addAll(tn1.getSchemaType());
        valTypes.addAll(tn2.getSchemaType());
        return valTypes;
    }

    @Override
    // the level of join is the maximum table level + 1
    public int getNestedQueryLevel() {
        int lv = 0;
        if (tn1.getNestedQueryLevel() > lv)
            lv = tn1.getNestedQueryLevel();
        if (tn2.getNestedQueryLevel() > lv)
            lv = tn2.getNestedQueryLevel();
        return lv + 1;
    }

    @Override
    public int getASTNodeCnt() {
        return 1 + this.tn1.getASTNodeCnt() + this.tn2.getASTNodeCnt() + this.joinKeys.size();
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        String result = this.tn1.prettyPrint(1, true).trim();
        result += " Left Outer Join \r\n" + this.tn2.prettyPrint(1, true);

        if (joinKeys.isEmpty())
            return IndentationManager.addIndention(result, indentLv);

        result += " On";

        List<String> joinConditions = joinKeys.stream()
                .map(p -> p.getKey() + " = " + p.getValue())
                .collect(Collectors.toList());

        result +=  " " + joinConditions.get(0);
        for (int i = 1; i < joinConditions.size(); i ++) {
            result += "\r\n\t And" + joinConditions.get(i);
        }

        return IndentationManager.addIndention(result, indentLv);
    }

    @Override
    public TableNode simplifyAST() {
        return new LeftJoinNode(tn1.simplifyAST(), tn2.simplifyAST(), this.joinKeys);
    }

    @Override
    public String toString() {
        return this.prettyPrint(0, false);
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        result.addAll(tn1.getAllHoles());
        result.addAll(tn2.getAllHoles());
        return result;
    }

    @Override
    public TableNode instantiate(InstantiateEnv env) {
        return new LeftJoinNode(
                this.tn1.instantiate(env),
                this.tn2.instantiate(env),
                this.joinKeys);
    }

    @Override
    public TableNode substNamedVal(ValNodeSubstitution vnsb) {
        return new LeftJoinNode(
                this.tn1.substNamedVal(vnsb),
                this.tn2.substNamedVal(vnsb),
                this.joinKeys);
    }

    public TableNode substCoreTable(TableNode leftCore, TableNode rightCore) {

        TableNode lrt = RenameWrapper.tryRename(leftCore);
        TableNode rrt = RenameWrapper.tryRename(rightCore);

        // rename the filters so that the filters refer to the elements in the new table.
        List<Pair<String, String>> stringNameBinding = new ArrayList<>();
        for (int i = 0; i < lrt.getSchema().size(); i ++) {
            stringNameBinding.add(new Pair<>(
                    this.tn1.getSchema().get(i),
                    lrt.getSchema().get(i)));
        }
        for (int i = 0; i < rrt.getSchema().size(); i ++) {
            stringNameBinding.add(new Pair<>(
                    this.tn2.getSchema().get(i),
                    rrt.getSchema().get(i)));
        }

        List<Pair<String, String>> newJoinKeys = new ArrayList<>();

        for (Pair<String, String> key : this.joinKeys) {
            String lKey = key.getKey();
            String rKey = key.getValue();

            for (Pair<String, String> p : stringNameBinding) {
                if (p.getKey().equals(key.getKey())) {
                    lKey = p.getValue();
                }
                if (p.getKey().equals(key.getValue())) {
                    rKey = p.getValue();
                }
            }
            newJoinKeys.add(new Pair<>(lKey, rKey));
        }

        return new LeftJoinNode(lrt, rrt, newJoinKeys);
    }

    @Override
    public List<NamedTableNode> namedTableInvolved() {
        List<NamedTableNode> result = new ArrayList<>();
        result.addAll(tn1.namedTableInvolved());
        result.addAll(tn2.namedTableInvolved());
        return result;
    }

    @Override
    public TableNode tableSubst(List<Pair<TableNode,TableNode>> pairs) {
        return new LeftJoinNode(
                tn1.tableSubst(pairs),
                tn2.tableSubst(pairs),
                joinKeys);
    }

    @Override
    public List<String> originalColumnName() {
        List<String> result = new ArrayList<>();
        result.addAll(tn1.originalColumnName());
        result.addAll(tn2.originalColumnName());
        return result;
    }

    public Pair<TableNode, TableNode> getTableNodes() { return new Pair<>(tn1, tn2); }

    @Override
    public double estimateAllFilterCost() {
        return tn1.estimateAllFilterCost() + tn2.estimateAllFilterCost();
    }

    @Override
    public List<Value> getAllConstants() {
        List<Value> result = new ArrayList<>();
        result.addAll(tn1.getAllConstants());
        result.addAll(tn2.getAllConstants());
        return result;
    }

    @Override
    public String getQuerySkeleton() {
        return "(LJ" + tn1.getQuerySkeleton() + " " + tn2.getQuerySkeleton() + ")";
    }

}
