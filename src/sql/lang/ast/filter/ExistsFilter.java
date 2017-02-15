package sql.lang.ast.filter;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.ast.table.TableNode;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.List;

/**
 * Created by clwang on 12/23/15.
 */
public class ExistsFilter implements Filter {

    // When this flag is true, this existComparator is NOT EXISTS
    private boolean notExists = false;
    TableNode tableNode;

    public ExistsFilter(TableNode tn) {
        this.tableNode = tn;
    }
    public ExistsFilter(TableNode tn, boolean notExists) { this.tableNode = tn; this.notExists = notExists; }

    public TableNode getTableNode() { return this.tableNode; }

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        Table table = tableNode.eval(env);
        if (table.getContent().isEmpty()) {
            if (notExists == true) {
                return true;
            }
            return false;
        }
        if (notExists == true) {
            return false;
        }
        return true;
    }

    @Override
    public int getFilterLength() {
        return 1;
    }

    @Override
    public int getNestedQueryLevel() {
        return tableNode.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        if (notExists == true) {
            return IndentionManagement.addIndention(
                    "Not Exists (\r\n" + tableNode.prettyPrint(1, false) + ")",
                    indentLv
            );
        }
        return IndentionManagement.addIndention(
                "Exists (\r\n" + tableNode.prettyPrint(1, false) + ")",
                indentLv
        );
    }

    @Override
    public boolean containsExclusiveFilter(Filter f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return tableNode.getAllHoles();
    }

    @Override
    public List<Value> getAllConstatnts() {
        return tableNode.getAllConstants();
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return new ExistsFilter(this.tableNode.instantiate(env), this.notExists);
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return new ExistsFilter(this.tableNode.substNamedVal(vnsb), this.notExists);
    }

}
