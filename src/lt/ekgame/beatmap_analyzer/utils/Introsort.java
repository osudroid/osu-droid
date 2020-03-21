package lt.ekgame.beatmap_analyzer.utils;

import java.util.List;

/*
 Hari Chinnan
hari.chinnan@gmail.com
713 248 6084 
 */

/*Generic IntroSort with the same method signature as Collections.sort method
 Introsort is an optimized sorting algorithm
 It begins with a QuickSort and Switches to HeapSort when the recursion depth 
 exceeds a set level based on the logarithm of the he number of elements being sorted. 
 It is the best of both worlds, with a worst-case O(n log n) runtime and practical performance 
 comparable to quicksort on typical data sets. Since both algorithms it uses are comparison 
 sorts, it too is a comparison sort.
 http://en.wikipedia.org/wiki/Introsort
 */

public final class Introsort {

    private static int size_threshold = 16;

    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        introSortLoop(list, 0, list.size(), 2 * floorLg(list.size()));

    }

    public static <T extends Comparable<? super T>> void sort(List<T> list, int begin, int end) {
        if (begin < end) {
            introSortLoop(list, begin, end, 2 * floorLg(end - begin));
        }
    }

    /*
     * Quicksort algorithm modified for Introsort
     */
    private static <T extends Comparable<? super T>> void introSortLoop(List<T> list, int lo, int hi, int depth_limit) {
        while (hi - lo > size_threshold) {
            if (depth_limit == 0) {
                heapsort(list, lo, hi);
                return;
            }
            depth_limit = depth_limit - 1;
            int p = partition(list, lo, hi, medianof3(list, lo, lo + ((hi - lo) / 2) + 1, hi - 1));
            introSortLoop(list, p, hi, depth_limit);
            hi = p;
        }
        insertionsort(list, lo, hi);
    }

    private static <T extends Comparable<? super T>> int partition(List<T> list, int lo, int hi, T x) {
        int i = lo, j = hi;
        while (true) {
            while (list.get(i).compareTo(x) < 0)
                i++;
            j = j - 1;
            while (x.compareTo(list.get(j)) < 0)
                j = j - 1;
            if (!(i < j))
                return i;
            exchange(list, i, j);
            i++;
        }
    }

    private static <T extends Comparable<? super T>> T medianof3(List<T> list, int lo, int mid, int hi) {

        if (list.get(mid).compareTo(list.get(lo)) < 0) {
            if (list.get(hi).compareTo(list.get(mid)) < 0)
                return list.get(mid);
            else {
                if (list.get(hi).compareTo(list.get(lo)) < 0)
                    return list.get(hi);
                else
                    return list.get(lo);
            }
        } else {
            if (list.get(hi).compareTo(list.get(mid)) < 0) {
                if (list.get(hi).compareTo(list.get(lo)) < 0)
                    return list.get(lo);
                else
                    return list.get(hi);
            } else
                return list.get(mid);
        }
    }

    /*
     * Heapsort algorithm
     */
    private static <T extends Comparable<? super T>> void heapsort(List<T> list, int lo, int hi) {
        int n = hi - lo;
        for (int i = n / 2; i >= 1; i = i - 1) {
            downheap(list, i, n, lo);
        }
        for (int i = n; i > 1; i = i - 1) {
            exchange(list, lo, lo + i - 1);
            downheap(list, 1, i - 1, lo);
        }
    }

    private static <T extends Comparable<? super T>> void downheap(List<T> list, int i, int n, int lo) {
        T d = list.get(lo + i - 1);
        int child;
        while (i <= n / 2) {
            child = 2 * i;
            if (child < n && (list.get(lo + child - 1).compareTo(list.get(lo + child)) < 0)) {
                child++;
            }
            if (d.compareTo(list.get(lo + child - 1)) >= 0)
                break;
            list.set(lo + i - 1, list.get(lo + child - 1));
            i = child;
        }
        list.set(lo + i - 1, d);
    }

    /*
     * Insertion sort algorithm
     */
    private static <T extends Comparable<? super T>> void insertionsort(List<T> list, int lo, int hi) {
        int i, j;
        T t;
        for (i = lo; i < hi; i++) {
            j = i;
            t = list.get(i);
            while (j != lo && t.compareTo(list.get(j - 1)) < 0) {
                list.set(j, list.get(j - 1));
                j--;
            }
            list.set(j, t);
        }
    }

    /*
     * Common methods for all algorithms
     */
    private static <T extends Comparable<? super T>> void exchange(List<T> list, int i, int j) {
        T t = list.get(i);
        list.set(i, list.get(j));
        list.set(j, t);
    }

    private static int floorLg(int a) {
        return (int) (Math.floor(Math.log(a) / Math.log(2)));
    }
}
