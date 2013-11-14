package com.user32.tjc.trails;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.*;

import android.app.Activity;
import android.app.Dialog;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity 
	extends FragmentActivity 
	implements LocationListener {

		public static GoogleMap map;
		private double currentLat = Double.NaN;
		private double currentLng = Double.NaN;
		private LatLng loveBldgLocation;
		private boolean markerSet = false;
		private boolean mapLoaded = false;
		private static final String TAG = "APPNAME";
		private Map<LatLng, String> startPoints;
		private boolean initialZoomDone = false;
		private float currentZoom;
		private boolean tracking = false;
		public static boolean startPicked = false;
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);
	        
	        startPoints = new HashMap<LatLng, String>();
	        startPoints.put(new LatLng(30.44527,-84.302394), "A");
	        startPoints.put(new LatLng(30.442692,-84.298343), "B");
	        startPoints.put(new LatLng(30.4461,-84.2993), "C");
	        
	        currentZoom = 15;
	        // Getting Google Play availability status
	        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

	        // Showing status
	        if(status != ConnectionResult.SUCCESS){ // Google Play Services are not available

	            int requestCode = 10;
	            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
	            dialog.show();

	        } else { // Google Play Services are available

	            // Getting reference to the SupportMapFragment of activity_main.xml
	            SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

	            // Getting GoogleMap object from the fragment
	            map = fm.getMap();
	            
	            map.setOnMapLoadedCallback(new OnMapLoadedCallback() {
					@Override
					public void onMapLoaded() {
						Log.e(TAG, "onMapLoaded");
						mapLoaded = true;
					}
				});
	            
	            // Enabling MyLocation Layer of Google Map
	            map.setMyLocationEnabled(true);

	            // Getting LocationManager object from System Service LOCATION_SERVICE
	            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	            // Creating a criteria object to retrieve provider
	            Criteria criteria = new Criteria();
	            // Getting the name of the best provider
	            String provider = locationManager.getBestProvider(criteria, true);
	            // Getting Current Location
	            Location location = locationManager.getLastKnownLocation(provider);

	            if(location != null){
	            	map.animateCamera(
			        		CameraUpdateFactory.newLatLngZoom(
			        				new LatLng(location.getLatitude(), location.getLongitude()), 
			        				18));
	            }
	            
	            locationManager.requestLocationUpdates(provider, 3000, 0, this);

	        }
	        
	    }

	    @Override
	    public void onLocationChanged(Location location) {

	    	
	    	Log.w(TAG, "onLocationChanged");

	    	if (mapLoaded == false) {
		    	Log.w(TAG, "Uh oh, map not loaded!");
	    		return;
	    		
	    	}

	    	//Get location
	    	currentLat = location.getLatitude();
	    	currentLng = location.getLongitude();

	    	Log.w(TAG, "Tracking!");
	        // Showing the current location in GoogleMap
	        
	    	if (startPicked  == true)
	    		map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLat, currentLng)));
	    }
	    

	    @Override
	    public void onProviderDisabled(String provider) {
	        // TODO Auto-generated method stub
	    }

	    @Override
	    public void onProviderEnabled(String provider) {
	        // TODO Auto-generated method stub
	    }

	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	        // TODO Auto-generated method stub
	    }

}