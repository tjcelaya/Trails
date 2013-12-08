package com.user32.tjc.trails;

import com.google.android.gms.maps.model.LatLng;

public class ConvertString {

	public static LatLng toLatLng(String s) {
		String[] coords = s.split(",");
		
		LatLng created_point = new LatLng(Double.valueOf(coords[0]), Double.valueOf(coords[1]));
		
		return created_point;
	}
}
