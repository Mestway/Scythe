package sql.lang.ast.filter;

import sql.lang.DataType.Value;
import sql.lang.ast.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by clwang on 12/14/15.
 */
public interface Filter {
    public boolean filter(Environment env);
}
