package sql.lang.ast;

import sql.lang.datatype.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by clwang on 12/16/15.
 */
public class Environment {
    Map<String, Value> env = new HashMap<String, Value>();

    public Environment() {};

    public Value lookup(String name) {
        if (env.containsKey(name)) {
            return env.get(name);
        }
        System.out.println("[Error@Environment22] " + "Empty key: " + name);
        return null;
    }

    public Environment extend(Map<String, Value> mapping) {
        Environment newEnv = this.copy();
        for (Map.Entry<String, Value> k : mapping.entrySet()) {
            newEnv.env.put(k.getKey(), k.getValue());
        }
        return newEnv;
    }

    private Environment copy() {
        Environment newEnv = new Environment();
        for (Map.Entry<String, Value> k : this.env.entrySet()) {
            newEnv.env.put(k.getKey(), k.getValue());
        }
        return newEnv;
    }
}
