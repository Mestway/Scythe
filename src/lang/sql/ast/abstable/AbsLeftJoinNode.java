package lang.sql.ast.abstable;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.table.Table;
import lang.table.TableRow;
import util.IndentationManager;
import util.Pair;
import util.RenameWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/18/15.
 * Join is implemented as cartesian product
 */
public class AbsLeftJoinNode extends AbsTableNode {

    AbsTableNode tn1, tn2;
    List<Pair<String, String>> joinKeys;

    public AbsLeftJoinNode(AbsTableNode tn1, AbsTableNode tn2, List<Pair<String, String>> joinKeys) {
        super();
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

        // the commented one is the original way of producing left-outer join tables,
        //Table t = Table.except(t1, tempTable.projection(projCols));
        //Table nullExtension = Table.extendWithNull(t, pl);

        // this is the one for the abstract language, since both of the tables may be filtered beforehand
        Table t = t1.duplicate();
        Table nullExtension = Table.extendWithNull(t, pl);

        return Table.union(tempTable, nullExtension);
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
    public AbsTableNode instantiate(InstantiateEnv env) {
        return new AbsLeftJoinNode(
                this.tn1.instantiate(env),
                this.tn2.instantiate(env),
                this.joinKeys);
    }

    @Override
    public AbsTableNode substNamedVal(ValNodeSubstitution vnsb) {
        return new AbsLeftJoinNode(
                this.tn1.substNamedVal(vnsb),
                this.tn2.substNamedVal(vnsb),
                this.joinKeys);
    }

    public AbsTableNode substCoreTable(AbsTableNode leftCore, AbsTableNode rightCore) {

        AbsTableNode lrt = RenameWrapper.tryRename(leftCore);
        AbsTableNode rrt = RenameWrapper.tryRename(rightCore);

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

        return new AbsLeftJoinNode(lrt, rrt, newJoinKeys);
    }

    @Override
    public List<AbsNamedTable> namedTableInvolved() {
        List<AbsNamedTable> result = new ArrayList<>();
        result.addAll(tn1.namedTableInvolved());
        result.addAll(tn2.namedTableInvolved());
        return result;
    }

    @Override
    public AbsTableNode tableSubst(List<Pair<AbsTableNode,AbsTableNode>> pairs) {
        return new AbsLeftJoinNode(
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

    public Pair<AbsTableNode, AbsTableNode> getTableNodes() { return new Pair<>(tn1, tn2); }

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
