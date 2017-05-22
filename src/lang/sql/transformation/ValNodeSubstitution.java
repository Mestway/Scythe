package lang.sql.transformation;

import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.ValNode;
import util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 1/25/16.
 * TODO: optimize this to be a datastructure using hash map
 */
public class ValNodeSubstitution {

    List<Pair<NamedVal, ValNode>> bindings = new ArrayList<>();

    public ValNodeSubstitution() {};

    /**
     * Look up the image for the given val node
     * if the image is contained in the bindings, return its image,
     * else return the original valNode (no update)
     * @param v
     * @return
     */
    public ValNode lookupImage(ValNode v) {
        for (Pair<NamedVal, ValNode> p : bindings) {
            if (p.getKey().equalsToValNode(v))
                return p.getValue();
        }
        return v;
    }

    public void addBinding(Pair<NamedVal, ValNode> p) {
        if (lookupImage(p.getKey()).equalsToValNode(p.getKey())) {
            bindings.add(p);
        } else {
            System.out.println(lookupImage(p.getKey()).prettyPrint(0) + " " + p.getKey().prettyPrint(0) );
            System.out.println("[ValNodeSubstBinding@37] Try to add a value of whose image is already inside: " + p.getKey().prettyPrint(0) + " " + p.getValue().prettyPrint(0));
            new Exception().printStackTrace();
        }
    }
}
