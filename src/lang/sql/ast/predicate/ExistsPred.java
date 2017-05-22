package lang.sql.ast.predicate;

import forward_enumeration.enum_components.parameterized.InstantiateEnv;
import lang.sql.ast.contable.TableNode;
import lang.sql.dataval.Value;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import util.IndentationManager;

import java.util.List;

/**
 * Created by clwang on 12/23/15.
 */
public class ExistsPred implements Predicate {

    // When this flag is true, this existComparator is NOT EXISTS
    private boolean notExists = false;
    TableNode tableNode;

    public ExistsPred(TableNode tn) {
        this.tableNode = tn;
    }
    public ExistsPred(TableNode tn, boolean notExists) { this.tableNode = tn; this.notExists = notExists; }

    public TableNode getTableNode() { return this.tableNode; }

    @Override
    public boolean eval(Environment env) throws SQLEvalException {
        Table table = tableNode.eval(env);
        if (table.getContent().isEmpty())
            return notExists;
        return ! notExists;
    }

    @Override
    public int getPredLength() {
        return 1;
    }

    @Override
    public int getNestedQueryLevel() {
        return tableNode.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        if (notExists == true) {
            return IndentationManager.addIndention(
                    "Not Exists \r\n" + tableNode.prettyPrint(1, true),
                    indentLv
            );
        }
        return IndentationManager.addIndention(
                "Exists \r\n" + tableNode.prettyPrint(1, true),
                indentLv
        );
    }

    @Override
    public boolean containRedundancy(Predicate f) { return false; }

    @Override
    public List<Hole> getAllHoles() {
        return tableNode.getAllHoles();
    }

    @Override
    public List<Value> getAllConstants() {
        return tableNode.getAllConstants();
    }

    @Override
    public Predicate instantiate(InstantiateEnv env) {
        return new ExistsPred(this.tableNode.instantiate(env), this.notExists);
    }

    @Override
    public Predicate substNamedVal(ValNodeSubstitution vnsb) {
        return new ExistsPred(this.tableNode.substNamedVal(vnsb), this.notExists);
    }

}
