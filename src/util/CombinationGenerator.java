package util;

import enumerator.parameterized.InstantiateEnv;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 1/7/16.
 */
public class CombinationGenerator {

    // Given a list of objects, return lists of all permutations of the object
    public static <T> List<List<T>> genCombination(List<T> list) {
        List<List<T>> result = new ArrayList<List<T>>();
        for (int sz = 1; sz <= list.size(); sz ++) {
            List<List<Integer>> indexPerm = genIndexCombination(0, list.size()-1, sz);
            for (List<Integer> indices : indexPerm) {
                List<T> oneList = new ArrayList<>();
                for (Integer i : indices) {
                    oneList.add(list.get(i));
                }
                result.add(oneList);
            }
        }
        return result;
    }

    public static <T> List<List<T>> genCombination(List<T> list, int n) {
        List<List<T>> result = new ArrayList<List<T>>();
        List<List<Integer>> indexPerm = genIndexCombination(0, list.size()-1, n);
        for (List<Integer> indices : indexPerm) {
            List<T> oneList = new ArrayList<>();
            for (Integer i : indices) {
                oneList.add(list.get(i));
            }
            result.add(oneList);
        }
        return result;
    }

    // given one size, retrieve all perms from that size
    private static List<List<Integer>> genIndexCombination(int l, int r, int size) {
        List<List<Integer>> lists = new ArrayList<List<Integer>>();
        if (r - l + 1 < size) { return lists; }

        if (size == 1) {
            for (int i = l; i <= r; i ++) {
                List<Integer> newList = new ArrayList<Integer>();
                newList.add(i);
                lists.add(newList);
            }
        } else {
            // find the left-most integer
            for (int i = l; i <= r; i ++) {
                List<List<Integer>> tails = genIndexCombination(i + 1, r, size - 1);
                for (List<Integer> t : tails) {
                    List<Integer> newList = new ArrayList<Integer>();
                    newList.add(i);
                    newList.addAll(t);
                    lists.add(newList);
                }
            }
        }
        return lists;
    }
}
