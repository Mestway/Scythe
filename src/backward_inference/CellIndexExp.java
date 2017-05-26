package backward_inference;

import lang.sql.dataval.NullVal;
import lang.sql.dataval.NumberVal;
import lang.sql.dataval.Value;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by clwang on 2/17/16.
 */
public class CellIndexExp {

    enum Operator {
        ADD(true), // add two arguments
        SUB(false), // subtraction
        IF_NULL(false), // if-then-else
        ID(false); // identity functions

        private Function<List<Value>, Value> f;
        private Function<List<Value>, Boolean> typeCheck;
        public final boolean commutative;

        Operator(boolean commutative) {
            this.commutative = commutative;
        }

        static {
            ID.f = l -> l.get(0);
            ID.typeCheck = l -> l.size() == 1;

            ADD.f = l -> new NumberVal(l.stream().map(x -> ((NumberVal) x).getVal()).reduce(0., Double::sum));
            ADD.typeCheck = l -> l.size() >= 2 && l.stream().map(x -> x instanceof NumberVal).reduce(true, Boolean::logicalAnd);

            SUB.f = l -> new NumberVal(((NumberVal) l.get(0)).getVal() - ((NumberVal) l.get(1)).getVal());
            SUB.typeCheck = l -> l.size() == 2 && l.get(0) instanceof NumberVal && l.get(1) instanceof NumberVal;

            IF_NULL.typeCheck = l -> (l.size() == 3 || l.size() == 2);
            IF_NULL.f = l -> (l.size() == 3 ? (l.get(0) instanceof NullVal ? l.get(1) : l.get(2))
                                            : (l.get(0) instanceof NullVal ? l.get(1) : l.get(0)));
        }

        public Value apply(List<Value> operands) {
            return this.f.apply(operands);
        }
        public Value apply(Value ... operands) {
            return this.apply(Arrays.asList(operands));
        }

        public boolean typeCheck(List<Value> operands) {
            return this.typeCheck.apply(operands);
        }
        public boolean typeCheck(Value ... operands) {
            return this.typeCheck(Arrays.asList(operands));
        }
    }

    Operator operator;
    List<CellIndex> operands = new ArrayList<>();

    public CellIndexExp(Operator op, CellIndex ... operands) {
        this(op, Arrays.asList(operands));
    }

    public CellIndexExp(Operator op, List<CellIndex> operands) {
        this.operator = op;
        for (int i = 1; i < operands.size(); i ++) {
            if (operands.get(i).r() != operands.get(i - 1).r()) {
                System.out.println("[ERROR@CellIndexExp27] Row num is not consistent");
                break;
            }
        }
        this.operands.addAll(operands);
    }

    @Override
    public int hashCode() {
        return operands.stream()
                .map(c -> c.hashCode())
                .reduce(0, (x, y) -> (x * 13 + y) % 15486433) * 13
                + operator.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CellIndexExp) {
            if (! (this.operator == ((CellIndexExp) obj).operator
                    && this.operands.size() == ((CellIndexExp) obj).operands.size()))
                return false;

            // pairwise equal
            return IntStream.range(0, this.operands.size())
                    .mapToObj(i -> ((CellIndexExp) obj).operands.get(i) == this.operands.get(i))
                    .reduce(true, (x, y)-> x && y);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.operator.toString()
                + "["
                + this.operands.stream().map(o -> o.toString()).reduce("", (x,y)->(x + "-" + y)).substring(1)
                + "]";
    }

    public CellIndexExp copy() {
        return new CellIndexExp(this.operator, this.operands.stream().map(o -> o.copy()).collect(Collectors.toList()));
    }

    /**
     * Get the expression over columns with the given CellIndexExp
     * @return A pair whose first element is the operator, the second element are columns involved
     */
    public Pair<Operator, List<Integer>> getColExp() {
        return new Pair<>(this.operator, this.operands.stream().map(ci -> ci.c()).collect(Collectors.toList()));
    }
    public int getRowIndex() {
        return this.operands.get(0).r();
    }

    // Check the consistency between two pairs
    public static boolean pairWiseConsistency(Pair<CellIndex, CellIndexExp> m1, Pair<CellIndex, CellIndexExp> m2) {

        if (m1.getKey().r() == m2.getKey().r()) {
            // their row should be consistent
            if (! (m1.getValue().operands.isEmpty() && m2.getValue().operands.isEmpty())) {
                if (m1.getValue().operands.get(0).r() != m2.getValue().operands.get(0).r()) {
                    return false;
                }
            }
        }

        if (m1.getKey().c() == m2.getKey().c()) {
            // this requires the operands to be of the same length and operators to be the same
            if (! (m1.getValue().operator.equals(m2.getValue().operator)
                    && m1.getValue().operands.size() == m2.getValue().operands.size()))
                return false;

            boolean columnConsistent = IntStream
                    .range(0, m1.getValue().operands.size())
                    .mapToObj(k -> m1.getValue().operands.get(k).c() == m2.getValue().operands.get(k).c())
                    .reduce(true, Boolean::logicalAnd);

            // column numbers of the operands should be consistent
            if (! columnConsistent)
                return false;
        }

        return true;
    }
}
