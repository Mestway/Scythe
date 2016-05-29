package sql.lang.ast.table;

import enumerator.context.EnumContext;
import enumerator.parameterized.InstantiateEnv;
import util.Pair;
import sql.lang.DataType.ValType;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.ast.Node;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by clwang on 12/16/15.
 */
public interface TableNode extends Node {
    Table eval(Environment env) throws SQLEvalException;

    List<String> getSchema();
    String getTableName();

    List<ValType> getSchemaType();
    int getNestedQueryLevel();
    //boolean equalsToTableNode(TableNode tn);

    String prettyPrint(int indentLv);
    List<Hole> getAllHoles();

    TableNode instantiate(InstantiateEnv env);
    TableNode substNamedVal(ValNodeSubstBinding vnsb);
    List<NamedTable> namedTableInvolved();

    // substitute a named table based on the cores
    TableNode tableSubst(List<Pair<TableNode,TableNode>> pairs);

    // the function will return a map,
    // that maps each column of this schema to its src (which column of which table)
    List<String> originalColumnName();

    static Map<String, String> nameToOriginMap(List<String> schema, List<String> originalNames) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < schema.size(); i ++) {
            map.put(schema.get(i), originalNames.get(i));
        }
        return map;
    }

    double estimateAllFilterCost();

    // Obtain the skeleton of a SQL query, the skeleton describes what does the query looks like,
    // this string can be used to distinguish query structure information
    String getQuerySkeleton();
}
