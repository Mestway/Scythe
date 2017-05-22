package lang.sql.ast.contable;

import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import util.Pair;
import util.IndentationManager;

import java.util.*;

/**
 * Created by clwang on 12/16/15.
 */
public class NamedTableNode extends TableNode {

    Table table;

    public NamedTableNode(Table table) {
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
    public int getASTNodeCnt() {
        return 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        if (asSubquery)
            return IndentationManager.addIndention(this.getTableName(), indentLv);
        else
            return IndentationManager.addIndention("Select * From " + this.getTableName(), indentLv);
    }

    @Override
    public TableNode simplifyAST() {
        return this;
    }

    @Override
    public List<Hole> getAllHoles() {
        return new ArrayList<>();
    }

    public Table getTable() { return this.table; }

    @Override
    public TableNode instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public TableNode substNamedVal(ValNodeSubstitution vnsb) {
        return this;
    }

    @Override
    public List<NamedTableNode> namedTableInvolved() {
        return Arrays.asList(this);
    }

    @Override
    public TableNode tableSubst(List<Pair<TableNode,TableNode>> pairs) {
        try {
            for (Pair<TableNode, TableNode> p : pairs)
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
        if (obj instanceof NamedTableNode) {
            return ((NamedTableNode) obj).table.equals(this.table);
        }
        return false;
    }

    @Override
    public double estimateAllFilterCost() {
        return 0;
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
