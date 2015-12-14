package org.opensha.commons.util;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Utility class for sorting a list of one object via another object.
 * @author kevin
 *
 * @param <C> the comparable object used for sorting
 * @param <E> the object that you want sorted via the given comparable
 */
public class ComparablePairing<C extends Comparable<C>, E> implements Comparable<ComparablePairing<C,E>> {
	
	private C comparable;
	private E data;
	
	public ComparablePairing(C comparable, E data) {
		this.comparable = comparable;
		this.data = data;
	}

	@Override
	public int compareTo(ComparablePairing<C,E> o) {
		return comparable.compareTo(o.comparable);
	}
	
	public E getData() {
		return data;
	}
	
	public C getComparable() {
		return comparable;
	}
	
	/**
	 * Creates a sortable list using the given list of comparables and data
	 * @param comparables
	 * @param datas
	 * @return
	 */
	public static <C extends Comparable<C>, E> List<ComparablePairing<C, E>> build(List<C> comparables, List<E> datas) {
		Preconditions.checkArgument(comparables.size() == datas.size());
		List<ComparablePairing<C, E>> list = Lists.newArrayList();
		
		for (int i=0; i<comparables.size(); i++) {
			list.add(new ComparablePairing<C, E>(comparables.get(i), datas.get(i)));
		}
		
		return list;
	}
	
	/**
	 * Returns a list of all elements in datas, sorted by the list of comparables
	 * @param comparables list by which data should be sorted
	 * @param datas list of data to be sorted
	 * @return
	 */
	public static <C extends Comparable<C>, E>  List<E> getSortedData(List<C> comparables, List<E> datas) {
		List<ComparablePairing<C, E>> list = build(comparables, datas);
		
		Collections.sort(list);
		
		List<E> sortedDatas = Lists.newArrayList();
		for (ComparablePairing<C, E> elem : list)
			sortedDatas.add(elem.getData());
		
		return sortedDatas;
	}

}
