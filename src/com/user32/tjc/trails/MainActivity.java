package com.user32.tjc.trails;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity 
	extends FragmentActivity 
	implements LocationListener {

		public static GoogleMap map;
		public static final float DETECT_DISTANCE = 80;
		public static final float CAPTURE_DISTANCE = 20;
		public static final String TAG = "APPNAME";
		public static Map<String, TupleArray<String, String>> trailnames_to_trails;
		public static TupleArray<Location, String> current_route;
		public static int last_visited = -1;
		public static Location current_location = null;
		public static Location next_location = null;
		public LatLng current_latlng = null;
		public LatLng next_latlng = null;
		public static float next_point_alpha = 0;
		public static float distance = 0;
		public static double currentLat = Double.NaN;
		public static double currentLng = Double.NaN;
		public static double nextPointLat = Double.NaN;
		public static double nextPointLng = Double.NaN;
		public static boolean map_loaded = false;
		public static boolean tracking = false;
		public static boolean finished_trail = false;
		public static boolean started = false;
		public static boolean picking = false;
		public static boolean pause_map_animation = false;
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);
	        
	        trailnames_to_trails = new HashMap<String, TupleArray<String,String>>();

	        current_route = new TupleArray<Location, String>();
	        current_location = new Location("class_static");
	        next_location = new Location("class_static");

	        // Getting Google Play availability status
	        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

	        // Showing status
	        if(status != ConnectionResult.SUCCESS){ // Google Play Services are not available
	            int requestCode = 10;
	            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
	            dialog.show();

	        } else { // Google Play Services are available

	            // Getting reference to the SupportMapFragment of activity_main.xml

	            // Getting GoogleMap object from the fragment
	            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
	            
	            //attaching callback anonymously. implementing it ourselves and passing `this` did not work
	            map.setOnMapLoadedCallback(new OnMapLoadedCallback() {
					@Override
					public void onMapLoaded() {
						Log.e(TAG, "onMapLoaded");
						MainActivity.map_loaded = true;
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
	            	map.moveCamera(
		        		CameraUpdateFactory.newLatLngZoom(
	        				new LatLng(location.getLatitude(), location.getLongitude()), 
	        				14));
	            }

	            RequestTrails r = new RequestTrails();
	            
	            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	                r.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (String[]) null);
	            else
	                r.execute();
 
	            locationManager.requestLocationUpdates(provider, 2000, 0, this);
	        }
	    }

		private class RequestTrails extends AsyncTask<String, Integer, String[]> {

			// @Override
			// protected void onPreExecute() { }
			
			@Override
			protected String[] doInBackground(String... params) {
				
				
				Log.e(TAG+"STARTASYNC", "doInBackground");
				
				HttpResponse response = null;
				String returned_json = null;
				String returned_routenames[] = null;
				HttpGet get = new HttpGet("http://trails-server.herokuapp.com/fetchRoutes");

				try {
					response = new DefaultHttpClient().execute(get);
					
					returned_json = EntityUtils.toString(response.getEntity());
				} catch (Exception e) {
					Log.e(TAG+"HTTPERR", "Couldn't reach the thing :(");
					e.printStackTrace();
				}

				if (returned_json != null)
					returned_routenames = attachFetchedRoutes(returned_json);
				
				if (returned_routenames != null)
					return returned_routenames;
				else
					return null;
			}

			@Override
			protected void onPostExecute(String[] addedRoutes) {

				Log.e(TAG+"END_ASYNC", "onPostExecute");
				
				if (addedRoutes == null) {
					Log.e(TAG+"-POST", "there was exception");
					return;
				}

				for (String s : addedRoutes) {
					Log.e(TAG+"-RT-POSTEXEC", s);
					map.addMarker(new MarkerOptions()
							.title(s)
							.position(ConvertString.toLatLng(trailnames_to_trails.get(s).k_list.get(0))));
				}
			}
		}
		
		public void selectJourney(View v) {
			
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            String provider = locationManager.getBestProvider(new Criteria(), true);
            current_location = locationManager.getLastKnownLocation(provider);

			if (current_location == null || map_loaded == false) {
				//cant start yet, we're missing one
				return;
			}

			picking = true;

			LatLngBounds.Builder b = new LatLngBounds.Builder();

			b.include(new LatLng(current_location.getLatitude(), current_location.getLongitude()));

			for (Map.Entry<String, TupleArray<String, String>> e : trailnames_to_trails.entrySet()) {

				//include startingpoint in bounds and draw it
				LatLng start_point = ConvertString.toLatLng(e.getValue().k_list.get(0));
				b.include(start_point);
				
				map.addMarker(new MarkerOptions()
						.position(start_point)
						.title(e.getKey()));
			}
			//fit screen to results
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 100));
			
			//remove title menu and show hint
			((View) findViewById(R.id.title_overlay)).setVisibility(View.GONE);
			((View) findViewById(R.id.adventure_tip)).setVisibility(View.VISIBLE);

			//set start listener
			map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
				@Override
				public void onInfoWindowClick(Marker arg0) {
					//Starting a trail
					MainActivity.picking = false;
					MainActivity.started = true;

					((View) findViewById(R.id.adventure_tip)).setVisibility(View.INVISIBLE);
					((View) findViewById(R.id.current_location)).setVisibility(View.VISIBLE);

					ParseTrailAndBegin r = new ParseTrailAndBegin();
		            
		            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		                r.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, arg0.getTitle());
		            else
		                r.execute(arg0.getTitle());
	 				
					map.setOnInfoWindowClickListener(null);		//should pause reframing
				}
			});
		}
		
		private class ParseTrailAndBegin extends AsyncTask<String, Void, Integer> {

			@Override
			protected Integer doInBackground(String... params) {
				if ( !trailnames_to_trails.containsKey(params[0]))
					return -1;
				
				Location l = new Location("parse_from_string");
				TupleArray<String, String> selectedRoute = trailnames_to_trails.get(params[0]);
				String[] point = new String[2];
				
				//put parsed route into current_route
				for (int i = 0, s = selectedRoute.k_list.size();
						i < s;
						++i) {
					
					point = selectedRoute.k_list.get(i).split(",", 2);
					l.setLatitude(Double.valueOf(point[0]));
					l.setLongitude(Double.valueOf(point[1]));
					
					current_route.add(
						new Location(l),
						selectedRoute.v_list.get(i));

					if (i == 1) {
						next_location = current_route.k_list.get(1);
						next_latlng = new LatLng(next_location.getLatitude(), next_location.getLongitude());
					}

				}

				last_visited = 0;				
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer error) {
				if (error != null)
					return;
				
				// draw first marker and exit
				map.addMarker(new MarkerOptions()
					.position(new LatLng(current_route.k_list.get(0).getLongitude(), current_route.k_list.get(0).getLongitude()))
					.title(current_route.v_list.get(0)));
			}
			
		}
		
		// should only be called in the background (from RequestTrails)
		public String[] attachFetchedRoutes(String json) {

			Log.w(TAG+"attachFetchedRoutes", json);
			
			JSONObject jsonObj;
			String tempRoute;
			String[] items = null;
			ArrayList<String> route_names = new ArrayList<String>();
			
			try {
				jsonObj = new JSONObject(json);
				for (Iterator<String> route = jsonObj.keys(); 
						route.hasNext();) {
					String key = route.next();
					
					Log.w(TAG+"attachFetchedRoutes-K", key);	
					Log.w(TAG+"attachFetchedRoutes-V", jsonObj.getString(key));	
					
					tempRoute = jsonObj.getString(key);
					
					if (tempRoute.contains("{BREAK_ITEM}"))
						items = tempRoute.split("\\{BREAK_ITEM\\}");
					else {
						continue;
					}
					TupleArray<String, String> route_definition = new TupleArray<String, String>();

					for (String i : items) {
						String point_and_clue[] = i.split("\\|BREAK_P\\|");
						
						Log.e(TAG+"split", point_and_clue[0] + " = " + point_and_clue[1]);
						
						route_definition.k_list.add(point_and_clue[0]);
						route_definition.v_list.add(point_and_clue[1]);
					}

					route_names.add(key);
					trailnames_to_trails.put(key, route_definition);
					
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			if (route_names.size() > 0)
				return (String[]) route_names.toArray(new String[route_names.size()]);
			else
				return null;
				
//			trailnames_to_trails.put(key, value)
		}
		
	    @Override
	    public void onLocationChanged(Location location) {

			if (map_loaded == false) {
				Log.e(TAG+"onLocationChanged", "Uh oh, map not loaded!");
				return;
			} else if (finished_trail == true || pause_map_animation == true)
				return;

			//Get location
			// currentLat = location.getLatitude();
			// currentLng = location.getLongitude();

			Log.w(TAG+"onLocationChanged", "Tracking!");
			
			if (started == true) {
				
				distance = location.distanceTo(next_location);
				
				Log.w(TAG+"onLocationChanged", "Started route, drawing next point");
				Log.e(TAG+"closEnough", "distance: "+distance+" C("+CAPTURE_DISTANCE+")");
				Log.e(TAG+"next", "l_v: "+last_visited+" F("+(current_route.size()-1)+")");

				
				//if captured final point, finish
				if (distance < CAPTURE_DISTANCE && last_visited+1 == current_route.size()) {
					//Finished!
					//draw ALL THE POINTS
					
					for (int i = 0, s = current_route.size();
							i < s; ++i)
						map.addMarker(new MarkerOptions()
								.position(new LatLng(current_route.k_list.get(i).getLatitude(), current_route.k_list.get(i).getLongitude()))
								.title(current_route.v_list.get(i)));
					
					map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
					finished_trail = true;
					Toast.makeText(this, "A WINRAR IS YOU", 1).show();
					
					return;

				//TODO if capture, advance next_location and last_visited, and return
				} else if (distance < CAPTURE_DISTANCE) {
					
					LatLng capturedPoint = new LatLng(next_location.getLatitude(), next_location.getLongitude());
					map.addMarker(new MarkerOptions()
							.position(capturedPoint)
							.title(current_route.v_list.get(last_visited++)));
					
					next_location = current_route.k_list.get(last_visited);
					next_latlng = new LatLng(next_location.getLatitude(), next_location.getLongitude());

				} else if (distance < DETECT_DISTANCE) {
					next_point_alpha = (DETECT_DISTANCE - location.distanceTo(next_location)) / DETECT_DISTANCE;
					map.clear();
					map.addMarker(new MarkerOptions()
							.alpha(next_point_alpha)
							.position(next_latlng));
				
					map.addCircle(new CircleOptions()
						.center(next_latlng)
						.radius(DETECT_DISTANCE)
						.strokeColor(Color.YELLOW));
					map.addCircle(new CircleOptions()
						.center(next_latlng)
						.radius(CAPTURE_DISTANCE)
						.strokeColor(Color.RED));
					
				}
				
				
				if (distance > DETECT_DISTANCE) {
					LatLngBounds bounds = LatLngBounds.builder()
							.include(new LatLng(location.getLatitude(), location.getLongitude()))
							.include(next_latlng)
							.build();
					map.clear();
					map.addCircle(new CircleOptions()
						.center(next_latlng)
						.radius(DETECT_DISTANCE)
						.strokeColor(Color.YELLOW));
					map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
				}
			 	
			} else if (picking == false && started == false) {
				Log.w(TAG+"onLocationChanged", "still haven't started");
				map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
			} else if (picking == true) {
				Log.w(TAG+"onLocationChanged", "we are picking, dont move");
			} else {
				Log.e(TAG+"onLocationChanged", "some other condition occurred");
				Log.e(TAG+"onLocationChanged", "started = "+started+" picking = "+picking);
			}
	    }

	    @Override
	    public void onProviderDisabled(String provider) { }

	    @Override
	    public void onProviderEnabled(String provider) { }

	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras) { }
	    
	    public void createJourney(View v) {
	    	
	    	
	    	Intent i = new Intent(this, PioneerMode.class);
	    	startActivity(i);
	    }
}