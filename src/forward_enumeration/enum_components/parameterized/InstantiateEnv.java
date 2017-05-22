package forward_enumeration.enum_components.parameterized;

import forward_enumeration.context.EnumContext;
import lang.sql.ast.valnode.ValHole;
import lang.sql.ast.valnode.ValNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 1/10/16.
 * The environment for subsituting holes with real values
 */
public class InstantiateEnv {

    private EnumContext ec = null;
    private List<ValNode> values = new ArrayList<>();

    public InstantiateEnv(List<ValNode> valNodes, EnumContext ec) {
        this.ec = ec;
        this.values = valNodes;
    }

    public ValNode substValHole(ValHole vh) {
        if (vh.getId() < values.size()) {
            // only do instantiation when type and id matches
            if (vh.getType(new EnumContext()).equals(values.get(vh.getId()).getType(ec)))
                return values.get(vh.getId());
        }
        return vh;
    }
}
