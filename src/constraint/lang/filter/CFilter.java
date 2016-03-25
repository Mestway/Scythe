package constraint.lang.filter;

import sql.lang.TableRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 3/6/16.
 */
public class CFilter {
    List<CBiComparator> comparators = new ArrayList<>();

    public CFilter(List<CBiComparator> filters) {
        this.comparators = filters;
    }

    public List<CBiComparator> getComparators() { return comparators; }

    public CFilter(CBiComparator ... filters) {
        for (CBiComparator f : filters)
            this.comparators.add(f);
    }

    public boolean eval(TableRow ourTr, TableRow inTr) {
        return comparators.stream()
                .map(c -> c.compare(ourTr, inTr))
                .reduce(true, (x, y) -> (x && y));
    }

    public String toString() {
        return comparators.stream().map(c -> c.toString()).reduce(">", (x, y) -> (x + ", " + y));
    }

}
