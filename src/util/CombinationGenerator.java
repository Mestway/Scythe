package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Created by clwang on 1/7/16.
 */
public class CombinationGenerator {

    // Given a list of objects, return a list containing all permutations of the object
    public static <T> List<List<T>> genCombination(List<T> list) {
        List<List<T>> result = new ArrayList<List<T>>();
        for (int sz = 1; sz <= list.size(); sz ++) {
            result.addAll(genCombination(list, sz));
        }
        return result;
    }

    public static <T> List<Pair<List<T>, List<T>>> genDecomposition(List<T> list) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < list.size(); i ++) indexes.add(i);

        List<Pair<List<T>, List<T>>> result = new ArrayList<Pair<List<T>, List<T>>>();

        List<List<Integer>> half = new ArrayList<List<Integer>>();
        for (int sz = 1; sz <= indexes.size() / 2; sz ++) {
            half.addAll(genCombination(indexes, sz));
        }

        for (List<Integer> li : half) {
            List<T> left = new ArrayList<T>();
            List<T> right = new ArrayList<T>();
            for (int i = 0; i < list.size(); i ++) {
                if (li.contains(i))
                    left.add(list.get(i));
                else
                    right.add(list.get(i));
            }
            result.add(new Pair<List<T>, List<T>>(left, right));
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

    /**
     * Generate permutations of a list with the allowance of one element appear multiple times
     * @param lst the list to generate permutation
     * @param n the size of permutation
     * @param <T> Type of the list
     * @return A list containing all permutations of the given element within length n
     */
    public static <T> List<List<T>> genMultPermutation(List<T> lst, int n) {
        if (n == 0) {
            List<List<T>> result = new ArrayList<>();
            result.add(new ArrayList<T>());
            return result;
        };

        List<List<T>> result = new ArrayList<>();
        List<List<T>> lastIteration = genMultPermutation(lst, n-1);

        for(List<T> tempList : lastIteration) {
            for (T t : lst) {
                List<T> newList = new ArrayList<>();
                newList.addAll(tempList);
                newList.add(t);
                result.add(newList);
            }
        }
        return result;
    }

    public static <T> List<List<T>> rotateList(List<List<T>> lst) {
        if (lst.size() == 1) {
            List<List<T>> result = new ArrayList<>();
            for (int i = 0; i < lst.get(0).size(); i ++)
                result.add(Arrays.asList(lst.get(0).get(i)));
            return result;
        }

        List<List<T>> last = rotateList(lst.subList(1, lst.size()));
        List<List<T>> result = new ArrayList<>();
        for (T t : lst.get(0)) {
            for (List<T> rt : last) {
                List<T> temp = new ArrayList<>();
                temp.add(t);
                temp.addAll(rt);
                result.add(temp);
            }
        }
        return result;
    }


    /**
     * Given a n-dimension integer list, generate all lists following the property below:
     * forall Result, i, Result[i] <= Bound[i]
     * @param bound a list containing boundary of the list
     * @return all lists with the above property
     */
    public static List<Vector<Integer>> genAllVectorLE(List<Integer> bound) {
        List<Vector<Integer>> result = new ArrayList<>();

        if (bound.size() == 0) {
            result.add(new Vector<>());
            return result;
        }

        List<Vector<Integer>> subListResult = genAllVectorLE(bound.subList(0, bound.size() - 1));
        Integer lastVal = bound.get(bound.size() - 1);
        for (int i = 0; i <= lastVal; i ++) {
            for (Vector<Integer> vec : subListResult) {
                Vector<Integer> newVec = (Vector<Integer>) vec.clone();
                newVec.add(i);
                result.add(newVec);
            }
        }

        return result;
    }

}
