package lang.sql.ast.abstable;

import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.table.Table;
import util.IndentationManager;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 12/16/15.
 */
public class AbsNamedTable extends AbsTableNode {

    Table table;

    public AbsNamedTable(Table table) {
        super();
        this.table = table;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        return table.duplicate();
    }

    @Override
    public List<String> getSchema() {
        return table.getQualifiedMetadata();
    }

    @Override
    public String getTableName() {
        return table.getName();
    }

    @Override
    public List<ValType> getSchemaType() {
        return table.getSchemaType();
    }

    @Override
    public int getNestedQueryLevel() {
        return 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        return IndentationManager.addIndention(this.getTableName(), indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        return new ArrayList<>();
    }

    public Table getTable() { return this.table; }

    @Override
    public AbsTableNode instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public AbsTableNode substNamedVal(ValNodeSubstitution vnsb) {
        return this;
    }

    @Override
    public List<AbsNamedTable> namedTableInvolved() {
        return Arrays.asList(this);
    }

    @Override
    public AbsTableNode tableSubst(List<Pair<AbsTableNode,AbsTableNode>> pairs) {
        try {
            for (Pair<AbsTableNode, AbsTableNode> p : pairs)
            if (this.table.contentEquals(p.getKey().eval(new Environment())))
                return p.getValue();
        } catch (SQLEvalException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public List<String> originalColumnName() {
        // original name is just its schema
        return this.getSchema();
    }

    @Override
    public int hashCode() {
        return this.table.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbsNamedTable) {
            return ((AbsNamedTable) obj).table.equals(this.table);
        }
        return false;
    }

    @Override
    public List<Value> getAllConstants() {
        return new ArrayList<>();
    }

    @Override
    public String getQuerySkeleton() {
        return "(N " + this.getTable().getName() + ")";
    }

}
