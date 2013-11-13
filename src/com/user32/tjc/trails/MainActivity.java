package com.user32.tjc.trails;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.*;

import android.app.Activity;
import android.app.Dialog;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class MainActivity extends FragmentActivity implements LocationListener {

		private GoogleMap map;
		private double currentLat;
		private double currentLng;
		private LatLng loveBldgLocation;
		private boolean markerSet;
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);
	        
	        loveBldgLocation = new LatLng(30.4461,-84.2993);
	        markerSet = false;
	        
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

	            if(location!=null){
	                onLocationChanged(location);
	            }
	            
	        	Log.w("Trails", "made it to check");

	            
	            
		        if (markerSet == false) {
		        	Log.w("Trails", "setting marker since we haven't");
		            map.addMarker(new MarkerOptions()
			            .position(loveBldgLocation)
			            .title("Love!"));
		            markerSet = true;
		        }
	            
	            locationManager.requestLocationUpdates(provider, 20000, 0, this);
	            
		        map.moveCamera(CameraUpdateFactory.newLatLng(loveBldgLocation));

		        map.animateCamera(CameraUpdateFactory.zoomTo(15));
		        
				new DelayedZoom().execute();

	        }
	    }
		
		private class DelayedZoom extends AsyncTask<Void, Void, Void> {
		
		    @Override
		    protected Void doInBackground(Void... v) {
	                try {
	                    Thread.sleep(1000);
	                } catch (InterruptedException e) {
	                    // TODO Auto-generated catch block
	                    e.printStackTrace();
	                }
	                return null;
		    }        
		
		    @Override
		    protected void onPostExecute(Void v) {             
		    }
		
		}
		
	    @Override
	    public void onLocationChanged(Location location) {

	        // Getting latitude of the current location
	        currentLat = location.getLatitude();

	        // Getting longitude of the current location
	        currentLng = location.getLongitude();

	        // Creating a LatLng object for the current location
	        LatLng latLng = new LatLng(currentLat, currentLng);

	        // Showing the current location in Google Map

	        LatLngBounds bounds = new LatLngBounds.Builder()
	        	.include(latLng)
	        	.include(loveBldgLocation)
	        	.build();
	        
	        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));
	        
	        // Zoom in the Google Map
	        map.animateCamera(CameraUpdateFactory.zoomTo(15));
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