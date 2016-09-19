package backward_inference;

/**
 * Created by clwang on 2/17/16.
 */
public class Coordinate {

    private int r; // r represents the row index
    private int c; // c represents the column index

    public static Coordinate genDummy() {
        return new Coordinate(-1, -1);
    }

    public boolean isDummy() {
        if (r == -1 && c == -1)
            return true;
        else
            return false;
    }

    public Coordinate(int row, int column) {
        this.r = row;
        this.c = column;
    }

    public int r() { return r; }
    public int c() { return c; }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coordinate) {
            if (((Coordinate) obj).c == this.c
                    && ((Coordinate) obj).r == this.r) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + r + ", " + c + ")";
    }

    public Coordinate copy() {
        return new Coordinate(this.r, this.c);
    }

}
