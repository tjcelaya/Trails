package com.user32.tjc.trails;
import java.util.ArrayList;

import com.google.android.gms.maps.internal.k;

public class TupleArray<K, V> {

	public ArrayList<K> k_list;
	public ArrayList<V> v_list;
	
	public TupleArray() {
		k_list = new ArrayList<K>();
		v_list = new ArrayList<V>();
	}

	
	public TupleArray(K key, V value) {
		k_list = new ArrayList<K>();
		v_list = new ArrayList<V>();

		k_list.add(key);
		v_list.add(value);
	}
	
	public void add(K k, V v) {
		k_list.add(k);
		v_list.add(v);
	}
	
	public int size() {
		return k_list.size();
	}
	
	public String toString() {
		String desc = "";
		
		for (int i = 0, size = k_list.size();
				i < size; i++)
		{
			desc += "\n\t" + k_list.get(i) + " C:" + v_list.get(i) + "\n";
		}
		
		
		return desc;
	}

}
