package sql.lang.trans;

import javafx.util.Pair;
import sql.lang.DataType.Value;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 1/25/16.
 */
public class ValNodeSubstBinding {

    List<Pair<NamedVal, ValNode>> bindings = new ArrayList<>();

    public ValNodeSubstBinding() {};

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
            System.out.println("[ValNodeSubstBinding@37] Try to add a value of whose image is already inside");
        }
    }
}
