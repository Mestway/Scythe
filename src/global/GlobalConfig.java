package global;

/**
 * Created by clwang on 5/12/16.
 */
public final class GlobalConfig {
    public static final boolean SPECIAL_TREAT_LAST_STAGE = true;

    public static final boolean SIMPLIFY_AGGR_FIELD = true;

    // the length of join keys in left-join nodes that will be enumerated
    public static final Integer LEFT_JOIN_KEY_LENGTH = 2;
}
