package sql.lang.ast.filter;

import enumerator.parameterized.InstantiateEnv;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.ast.table.TableNode;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.List;

/**
 * Created by clwang on 12/23/15.
 */
public class ExistComparator implements Filter {
    TableNode tableNode;

    public ExistComparator(TableNode tn) {
        this.tableNode = tn;
    }

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        Table table = tableNode.eval(env);
        if (table.getContent().isEmpty()) {
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
        return IndentionManagement.addIndention(
                "EXIST (\r\n" + tableNode.prettyPrint(1) + ")",
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
    public Filter instantiate(InstantiateEnv env) {
        return new ExistComparator(this.tableNode.instantiate(env));
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return new ExistComparator(this.tableNode.substNamedVal(vnsb));
    }

}
