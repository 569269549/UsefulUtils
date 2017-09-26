/*
  Created by cox
  on 12/05/15.
 */
package com.xiaomi.oga.utils;

import android.os.Build;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LongSparseArray;
import android.util.SparseArray;

import com.xiaomi.oga.main.timeline.MainTimelineItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class ContainerUtil {

    private static final ArrayList<Object> NULL_LIST = new ArrayList<Object>() {
        @Override
        public boolean add(Object object) {
            throw new UnsupportedOperationException("ContainerUtil : Unsupported add operation on const empty list");
        }

        @Override
        public boolean addAll(Collection<?> collection) {
            throw new UnsupportedOperationException("ContainerUtil : Unsupported addAll operation on const empty list");
        }

        @Override
        public boolean addAll(int index, Collection<?> collection) {
            throw new UnsupportedOperationException("ContainerUtil : Unsupported addAll operation on const empty list");
        }

        @Override
        public void add(int index, Object object) {
            throw new UnsupportedOperationException("ContainerUtil : Unsupported add by index operation on const empty list");
        }
    };

    private ContainerUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> ArrayList<T> getEmptyArrayList() {
        return (ArrayList<T>) NULL_LIST;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getEmptyList() {
        return (List<T>) NULL_LIST;
    }

    public static <T> boolean hasData(Collection<T> list) {
        return list != null && !list.isEmpty();
    }

    public static <T> boolean hasData(LongSparseArray<T> list) {
        return list != null && list.size() > 0;
    }

    public static <T> boolean hasData(android.support.v4.util.LongSparseArray<T> list) {
        return list != null && list.size() > 0;
    }

    public static <T> boolean hasData(SparseArray<T> array) {
        return array != null && array.size() > 0;
    }

    public static <T> boolean hasData(int[] list) {
        return list != null && list.length > 0;
    }

    public static <T> boolean isEmpty(Collection<T> list) {
        return !hasData(list);
    }

    public static <T> boolean isEmpty(SparseArray<T> array) {
        return !hasData(array);
    }

    public static <T> T getFirst(Collection<T> list) {
        return hasData(list) ? list.iterator().next() : null;
    }

    public static <T> long getFirst(long[] list, int defaultValue) {
        return hasData(list) ? list[0] : defaultValue;
    }

    public static <T> int getFirst(int[] list, int defaultValue) {
        return hasData(list) ? list[0] : defaultValue;
    }

    public static <T> boolean isEmpty(int[] list) {
        return !hasData(list);
    }

    public static <T> boolean isEmpty(long[] list) {
        return !hasData(list);
    }

    public static <T> boolean isEmpty(float[] list) {
        return !hasData(list);
    }

    public static boolean hasData(long[] list) {
        return list != null && list.length > 0;
    }

    public static <T> boolean hasData(float[] array) {
        return array != null && array.length != 0;
    }

    public static <K, V> int getSize(Map<K, V> list) {
        return hasData(list) ? list.size() : 0;
    }

    public static <T> int getSize(Collection<T> list) {
        return hasData(list) ? list.size() : 0;
    }

    public static <T> int getSize(T[] array) {
        return hasData(array) ? array.length : 0;
    }

    public static <T> int getSize(int[] array) {
        return hasData(array) ? array.length : 0;
    }

    public static <T> int getSize(long[] array) {
        return hasData(array) ? array.length : 0;
    }

    public static int getSize(boolean[] a) {
        return hasData(a) ? a.length : 0;
    }

    public static int getSize(byte[] bytes) {
        return isEmpty(bytes) ? 0 : bytes.length;
    }

    public static int getSize(float[] floats) {
        return isEmpty(floats) ? 0 : floats.length;
    }

    public static <K, V> boolean hasData(Map<K, V> map) {
        return map != null && !map.isEmpty();
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return !hasData(map);
    }

    public static <T> Collection<T> removeEmpties(Collection<T> list) {
        if (isEmpty(list)) {
            return list;
        }
        List<T> ret = new ArrayList<T>();
        for (T e : list) {
            if (e != null) {
                ret.add(e);
            }
        }

        return ret;
    }

    public static boolean isEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }

    public static boolean hasData(CharSequence s) {
        return !ContainerUtil.isEmpty(s);
    }

    public static <T> boolean isEmpty(T[] array) {
        return !hasData(array);
    }

    public static boolean isEmpty(boolean[] array) {
        return !hasData(array);
    }

    public static boolean isEmpty(byte[] array) {
        return !hasData(array);
    }

    public static <T> boolean hasData(T[] array) {
        return array != null && array.length != 0;
    }

    public static boolean hasData(boolean[] array) {
        return array != null && array.length != 0;
    }

    public static boolean hasData(byte[] array) {
        return array != null && array.length != 0;
    }

    public static <T> ArrayList<T> toArrayList(T[]... arrayList) {
        if (arrayList == null) {
            return null;
        }

        ArrayList<T> ret = new ArrayList<T>();
        for (T[] array : arrayList) {
            if (!isEmpty(array)) {
                Collections.addAll(ret, array);
            }
        }
        return ret;
    }

    public static <T> ArrayList<T> toArrayList(T[] arrayList) {
        return toArrayList(arrayList, null);
    }

    public static boolean hasData(JSONObject jsonObject) {
        return jsonObject != null && hasData(jsonObject.names());
    }

    public static boolean isEmpty(JSONObject jsonObject) {
        return !hasData(jsonObject);
    }

    public static boolean hasData(JSONArray jsonArray) {
        return jsonArray != null && jsonArray.length() != 0;
    }

    public static boolean isEmpty(JSONArray jsonArray) {
        return !hasData(jsonArray);
    }

    public static <T> boolean isOutOfBound(long index, Collection<T> collection) {
        return index < 0 || index >= ContainerUtil.getSize(collection);
    }

    public static <T> boolean isOutOfBound(long index, T[] array) {
        return index < 0 || index >= ContainerUtil.getSize(array);
    }

    public static boolean isOutOfBound(long index, long[] array) {
        return index < 0 || index >= ContainerUtil.getSize(array);
    }

    public static <T> int getSize(SparseArray<T> items) {
        return items != null ? items.size() : 0;
    }

    public interface ElementComparator<T1, T2> {
        boolean isDifferent(T1 lhs, T2 rhs);
    }

    public static <T1, T2> boolean isListDifferent(List<T1> list1, List<T2> list2, ElementComparator<T1, T2> comparator) {
        boolean isDifferent = true;
        int count1 = ContainerUtil.getSize(list1);
        int count2 = ContainerUtil.getSize(list2);
        if (count1 == count2) {
            isDifferent = false;
            for (int i = 0; i < count1; i++) {
                T1 prev = list1.get(i);
                T2 cur = list2.get(i);
                if (comparator.isDifferent(prev, cur)) {
                    isDifferent = true;
                    break;
                }
            }
        }
        return isDifferent;
    }

    public static ArrayList<Integer> toArrayList(int[] integers) {
        ArrayList<Integer> ret = new ArrayList<Integer>(ContainerUtil.getSize(integers));
        if (ContainerUtil.isEmpty(integers)) {
            return ret;
        }
        for (int integer : integers) {
            ret.add(integer);
        }
        return ret;
    }

    public static ArrayList<Long> toArrayList(long[] numbers) {
        ArrayList<Long> ret = new ArrayList<Long>(ContainerUtil.getSize(numbers));
        if (ContainerUtil.isEmpty(numbers)) {
            return ret;
        }
        for (long integer : numbers) {
            ret.add(integer);
        }
        return ret;
    }

    public static <T> ArrayList<T> copy(List<T> source) {
        if (ContainerUtil.isEmpty(source)) {
            return null;
        }
        return new ArrayList<>(source);
    }

    public static <T> boolean contains(T[] array, T obj) {
        if (array != null && obj != null) {
            for (T item : array) {
                if (item.equals(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    public enum SizeType {
        SIZE_LESS_THAN_1000,
        SIZE_NOT_LESS_THAN_1000,
        SIZE_UNKNOWN,
    }

    /**
     * Create proper map for better performance.
     *
     * @param estimateSize SIZE_LESS_THAN_1000 or SIZE_NOT_LESS_THAN_1000.
     *                     Wrong argument won't cause correctness bug.
     * @return Map, not thread-safe.
     */
    public static <K, V> Map<K, V> createMap(SizeType estimateSize) {
        return isArraySetOrMapProvidedBySystem(estimateSize, getMinVersionOfArrayMap()) ? new ArrayMap<K, V>() : new HashMap<K, V>();
    }

    public static <K, V> Map<K, V> createMapWithCapacity(int capacity) {
        int capacityPositive = Math.max(1, capacity);
        return isArraySetOrMapProvidedBySystem(capacity, getMinVersionOfArrayMap()) ? new ArrayMap<K, V>(capacityPositive) : new HashMap<K, V>(capacityPositive);
    }

    private static int getMinVersionOfArrayMap() {
        return Build.VERSION_CODES.KITKAT;
    }

    public static <T> Set<T> createSet(SizeType estimateSize) {
        // If system build SDK is bigger than 23, we can use ArraySet to improve performance for 23 and above.
        return isArraySetOrMapProvidedBySystem(estimateSize, getMinVersionOfArraySet()) ? new ArraySet<T>() : new HashSet<T>();
    }

    public static <T> Set<T> createSet(int capacity) {
        return isArraySetOrMapProvidedBySystem(capacity, getMinVersionOfArraySet()) ? new ArraySet<T>() : new HashSet<T>();
    }

    private static int getMinVersionOfArraySet() {
        return Build.VERSION_CODES.M;
    }

    private static boolean isArraySetOrMapProvidedBySystem(int capacity, int requiredVersion) {
        return isArraySetOrMapProvidedBySystem(capacity < 1000 ? SizeType.SIZE_LESS_THAN_1000 : SizeType.SIZE_NOT_LESS_THAN_1000, requiredVersion);
    }

    private static boolean isArraySetOrMapProvidedBySystem(SizeType estimateSize, int requiredVersion) {
        return estimateSize == SizeType.SIZE_LESS_THAN_1000 && Build.VERSION.SDK_INT >= requiredVersion;
    }

    public static int getFirstPositive(int... args) {
        for (int e : args) {
            if (e != 0) {
                return e;
            }
        }
        return 0;
    }

    public static float getFirstNoLessThan(float minimum, float... args) {
        for (float e : args) {
            if (e >= minimum) {
                return e;
            }
        }
        return minimum;
    }
}