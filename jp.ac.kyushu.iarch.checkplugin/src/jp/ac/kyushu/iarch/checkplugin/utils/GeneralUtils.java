package jp.ac.kyushu.iarch.checkplugin.utils;

import java.util.ArrayList;
import java.util.List;

public class GeneralUtils {
	
	private GeneralUtils() {}
	
	public static <T, U extends T, V extends T> List<T> joinLists(List<U> list1, List<V> list2) {
		List<T> result = new ArrayList<>();
		result.addAll(list1);
		result.addAll(list2);
		return result;
	}
	
	public static <T, U extends T, V extends T> List<T> joinElemAndList(U elem, List<V> list) {
		List<T> result = new ArrayList<>();
		result.add(elem);
		result.addAll(list);
		return result;
	}

}
