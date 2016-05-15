package global;

/**
 * Created by clwang on 5/12/16.
 */
public class Statistics {

    // The following two is to demonstrate the effectiveness

    // To calculate the average reduction rate of primitive filters (in different syntax form) to their bitvector representations.
    // including: 1) primitives filters for basic input tables
    //            2) primitive canonical filters for aggregation queries (one of the fields must be aggregation field)
    //            3) primitive canonical filters for
    public static double sum_red_syn_to_bv = 0;
    public static int red_syn_to_bv_case_cnt = 0;



    // These three different from the later part in the sense that they consider all stages,
    // and the later part only consider the last stage

    // The average reduction rate of different combinations of filters to their bitvector representations
    public static double sum_red_compound_to_bv = 0;
    // To calculate how many compound filters are there to be visited
    public static double sum_compound_filter_cnt = 0;
    public static int max_compound_filter_cnt = 0;
    public static int compound_case_cnt = 0;

    // The following four are used to demonstrate the effectiveness of backward inference.

    // To calculate the average number of filters came from back inference
    public static double sum_back_infer_filter_size = 0;
    public static int max_back_infer_filter_size = 0;
    // To calculate on average, how many of the backward inferred queries are bogus.
    public static double sum_back_filter_bogus_rate = 0;
    public static double back_filter_bogus_cases = 0;
    // To calculate how many filters are actually visited during this process.
    public static double sum_last_stage_visited_filter_cnt = 0;
    public static double sum_last_stage_compound_filter_cnt = 0;
    public static double sum_last_stage_red_visited_compound = 0;
    public static int last_stage_compound_case_cnt = 0;


    public static void printAllStatistics() {
        System.out.println("[avg reduction rate: syn filter / bit vector] " + sum_red_syn_to_bv / red_syn_to_bv_case_cnt);
        System.out.println("[avg reduction rate: compound filter / bit vector] " + sum_red_compound_to_bv / compound_case_cnt);
        System.out.println("[avg compound filter count] " + sum_compound_filter_cnt / compound_case_cnt);
        System.out.println("[max compound filter count] " + max_compound_filter_cnt);

        if (last_stage_compound_case_cnt == 0)
            return;

        System.out.println();
        System.out.println("[avg last stage compound filter count] "
                + sum_last_stage_compound_filter_cnt / last_stage_compound_case_cnt);
        System.out.println("[avg visited filter count] "
                + sum_last_stage_visited_filter_cnt / last_stage_compound_case_cnt);
        System.out.println("[avg back inferred filter count] "
                + sum_back_infer_filter_size / last_stage_compound_case_cnt);
        System.out.println("[max back inferred filter count] "
                + max_back_infer_filter_size);
        System.out.println("[avg reduction visited compound filters rate (as using back inference)] "
                + sum_last_stage_red_visited_compound / last_stage_compound_case_cnt );
        System.out.println("[avg back inferred filter bogus rate] " + sum_back_filter_bogus_rate / back_filter_bogus_cases);
    }
}
