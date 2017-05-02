package backward_inference;

/**
 * Created by clwang on 2/17/16.
 */
public class CellIndex {

    private int r; // r represents the row index
    private int c; // c represents the column index

    public static CellIndex initDummyCellIndex() {
        return new CellIndex(-1, -1);
    }

    public boolean isDummy() {
        if (r == -1 && c == -1)
            return true;
        else
            return false;
    }

    public CellIndex(int row, int column) {
        this.r = row;
        this.c = column;
    }

    public int r() { return r; }
    public int c() { return c; }

    @Override
    public int hashCode() {
        return r * 13 + c;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CellIndex) {
            if (((CellIndex) obj).c == this.c
                    && ((CellIndex) obj).r == this.r) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + r + ", " + c + ")";
    }
    public CellIndex copy() {
        return new CellIndex(this.r, this.c);
    }
}
