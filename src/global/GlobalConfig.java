package global;

/**
 * Created by clwang on 5/12/16.
 */
public final class GlobalConfig {

    public static final boolean TRY_NATURAL_JOIN = true;

    public static final boolean SIMPLIFY_AGGR_FIELD = true;

    // the length of join keys in left-join nodes that will be enumerated
    public static final Integer LEFT_JOIN_KEY_LENGTH = 2;

    public static final Integer MAXIMUM_FILTER_LENGTH = 2;

    // the number of top queries stored during the decoding process
    public static final Integer MAXIMUM_BEAM_SIZE = 10;

    // we will only try perform decomposition only when the number is less than 5
    public static final boolean TRY_DECOMPOSITION = true;
    public static final Integer TRY_DECOMPOSE_ROW_NUM = 8;

    public static final Integer MAXIMUM_QUERY_KEPT = 5;

    public static final Integer DESIRABLE_CANDIDATE_QUERY_SCORE = 10;

    public static final boolean STAT_MODE = false;

    public static final boolean PRINT_LOG = true;

    public static final boolean GUESS_ADDITIONAL_CONSTANTS = false;

}
