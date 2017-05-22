package lang.sql.ast.abstable;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.ast.Node;
import lang.sql.dataval.*;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import lang.table.Table;
import util.Pair;
import util.RenameWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/16/15.
 */
public abstract class AbsTableNode implements Node {

    private static int globalTableNodeId = 0;

    private int nodeId;

    public AbsTableNode() {
        this.nodeId = globalTableNodeId;
        globalTableNodeId ++;
    }
    public String getNodeId() { return "T" + this.nodeId; }

    public abstract Table eval(Environment env) throws SQLEvalException;

    public abstract List<String> getSchema();
    public abstract String getTableName();

    public abstract List<ValType> getSchemaType();
    public abstract int getNestedQueryLevel();
    //boolean equalsToTableNode(TableNode tn);

    public List<Pair<String, ValType>> getSchemaWithType() {
        List<Pair<String, ValType>> result = new ArrayList<>();
        List<String> schema = this.getSchema();
        List<ValType> schemaType = this.getSchemaType();
        for (int i = 0; i < schema.size(); i ++) {
            result.add(new Pair<>(schema.get(i), schemaType.get(i)));
        }
        return result;
    }

    public abstract String prettyPrint(int indentLv, boolean asSubquery);
    public String printQuery() {
        String query = this.prettyPrint(0, false) + ";";
        Set<String> generatedNames = RenameWrapper.findAllGeneratedNames(query)
                .stream().collect(Collectors.toSet());

        int i = 1;
        for (String s : generatedNames) {
            while (query.contains("t" + i))
                i ++;
            query = query.replace(s, "t" + i);
            i ++;
        }

        return query;
    };

    public abstract List<Hole> getAllHoles();

    public abstract AbsTableNode instantiate(InstantiateEnv env);
    public abstract AbsTableNode substNamedVal(ValNodeSubstitution vnsb);
    public abstract List<AbsNamedTable> namedTableInvolved();

    // substitute a named table based on the cores
    public abstract AbsTableNode tableSubst(List<Pair<AbsTableNode,AbsTableNode>> pairs);

    // the function will return a map,
    // that maps each column of this schema to its src (which column of which table)
    public abstract List<String> originalColumnName();

    public static Map<String, String> nameToOriginMap(List<String> schema, List<String> originalNames) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < schema.size(); i ++) {
            map.put(schema.get(i), originalNames.get(i));
        }
        return map;
    }

    public abstract List<Value> getAllConstants();

    // Obtain the skeleton of a SQL query, the skeleton describes what does the query looks like,
    // this string can be used to distinguish query structure information
    public abstract String getQuerySkeleton();
}
