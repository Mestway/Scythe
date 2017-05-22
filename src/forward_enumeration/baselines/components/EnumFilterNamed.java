package forward_enumeration.baselines.components;

import forward_enumeration.context.EnumContext;
import forward_enumeration.QueryContainer;
import forward_enumeration.enum_components.FilterEnumerator;
import lang.table.Table;
import lang.sql.dataval.ValType;
import lang.sql.ast.Environment;
import lang.sql.ast.predicate.Predicate;
import lang.sql.ast.contable.NamedTableNode;
import lang.sql.ast.contable.SelectNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.ast.valnode.ValNode;
import lang.sql.exception.SQLEvalException;
import util.RenameWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumerate filtered named tables from enum context
 * Created by clwang on 2/1/16.
 */
public class EnumFilterNamed {

    /**
     * Enumerate eval nodes for named tables: given a named table, we will generate eval for the named tables.
     * @return
     */
    public static List<TableNode> enumFilterNamed(EnumContext ec,  boolean allowDisjunction) {
        List<TableNode> targets = ec.getTableNodes().stream()
                .filter(tn -> (tn instanceof NamedTableNode))
                .collect(Collectors.toList());

        List<TableNode> result = new ArrayList<>();

        for (TableNode tn : targets) {

            // the selection args are complete
            List<ValNode> vals = tn.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());

            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

            // we allow using exists when enumerating filters for a named table.
            boolean allowExists = true;
            List<Predicate> filters = FilterEnumerator.enumFiltersLR(vals, ec2.getValNodes(), ec2, allowExists, allowDisjunction);

            for (Predicate f : filters) {
                TableNode sn = new SelectNode(vals, tn, f);
                result.add(sn);
            }
        }
        return result;
    }

    // Emit enumerated query on the fly, whether to store them or not is determined by qc
    public static List<Table> emitEnumFilterNamed(EnumContext ec, QueryContainer qc, boolean allowDisjunction) {

        Set<Table> newlyGeneratedTables = new HashSet<>();

        List<TableNode> targets = ec.getTableNodes();

        for (TableNode tn : targets) {

            // the selection args are complete
            List<ValNode> vals = tn.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());

            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

            boolean allowExists = true;
            List<Predicate> filters = FilterEnumerator.enumFiltersLR(vals, ec2.getValNodes(), ec2, allowExists, allowDisjunction);

            for (Predicate f : filters) {
                TableNode sn = new SelectNode(vals, tn, f);
                // when a table is generated, emit it to the query chest
                // inserting an edge from eval(tn) --> eval(sn)

                if (qc.getContainerType() == QueryContainer.ContainerType.TableLinks) {

                    // if qc use eval links, we can put eval links into qc
                    try {
                        Table src = tn.eval(new Environment());
                        Table dst = sn.eval(new Environment());

                        if (src.getContent().size() == 0 || dst.getContent().size() == 0)
                            continue;

                        qc.insertQuery(RenameWrapper.tryRename(sn));
                        qc.getTableLinks().insertEdge(
                                qc.getRepresentative(src),
                                qc.getRepresentative(dst));

                        newlyGeneratedTables.add(qc.getRepresentative(dst));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return newlyGeneratedTables.stream().collect(Collectors.toList());
    }
}
