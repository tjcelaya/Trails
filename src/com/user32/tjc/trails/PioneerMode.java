package com.user32.tjc.trails;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PioneerMode 
	extends FragmentActivity
	implements LocationListener {

	public static GoogleMap map;
	public static final float DETECT_DISTANCE = 80;
	public static final float CAPTURE_DISTANCE = 20;
	public static final String TAG = "PIONEER";
	public static LocationManager location_manager = null;
	public static Location last_point_location = null;
	public static Location current_location = null;
	public static float distance = 0;
	public static double currentLat = Double.NaN;
	public static double currentLng = Double.NaN;
	public static boolean map_loaded = false;
	public static boolean started = false;
	public static boolean itemAdded = false;

	public static ProgressDialog upload_dialog = null;
	public static TrailsPost post_asynctask = null;

	public static String constructedRoute = "";
	public static String routeName = "";
	
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pioneer);
       
    	location_manager = null;
    	last_point_location = null;
    	current_location = null;
    	distance = 0;
    	currentLat = Double.NaN;
    	currentLng = Double.NaN;
    	map_loaded = false;
    	started = false;
    	itemAdded = false;
        
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
//            map.setOnMapLoadedCallback(new OnMapLoadedCallback() {
//				@Override
//				public void onMapLoaded() {
//					Log.e(TAG, "onMapLoaded");
//				}
//			});
            
            // Enabling MyLocation Layer of Google Map
            map.setMyLocationEnabled(true);

            // Getting LocationManager object from System Service LOCATION_SERVICE
            location_manager = (LocationManager) getSystemService(LOCATION_SERVICE);
            
            // Creating a criteria object to retrieve provider
            Criteria criteria = new Criteria();
            // Getting the name of the best provider
            String provider = location_manager.getBestProvider(criteria, true);
            // Getting Current Location
            Location location = location_manager.getLastKnownLocation(provider);

            
            
            if(location != null){
            	map.moveCamera(
	        		CameraUpdateFactory.newLatLngZoom(
        				new LatLng(location.getLatitude(), location.getLongitude()), 
        				14));
            	
	            	currentLat = location.getLatitude();
	            currentLng = location.getLongitude();
            }

            location_manager.requestLocationUpdates(provider, 1000, 0, this);
            current_location = new Location("class_static");
        }
	}
	
	public void addPoint(View v) {
		AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
		
		final EditText clueInput = new EditText(this);
		
		dialogbuilder.setMessage("Enter a clue:")
			.setTitle("Add Checkpoint")
			.setView(clueInput)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.w(TAG, clueInput.getText().toString());
					
					if (Double.isNaN(currentLat) || Double.isNaN(currentLng))
						return;
					else if (last_point_location != null && current_location.distanceTo(last_point_location) < 20) {
						Toast.makeText(getApplicationContext(), "Too close to previous point", Toast.LENGTH_LONG).show();
						
						return;
					}
					
					
					constructedRoute += Double.toString(currentLat) + "," + Double.toString(currentLng);
					constructedRoute += "|BREAK_P|" + clueInput.getText().toString().replaceAll("\"", "\\\"") + "{BREAK_ITEM}";
					Log.w(TAG, "constructed route is now: " + constructedRoute);
					last_point_location = new Location(current_location);
					last_point_location.setLatitude(currentLat);
					last_point_location.setLongitude(currentLng);
					
					map.addMarker(new MarkerOptions().position(new LatLng(currentLat, currentLng)));
					
					if (!itemAdded)
						itemAdded = true;
					
				}
			})
			.setNegativeButton("Cancel", null).show();
	}
	
	public void publishPath(View v) {
		AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
		
		final EditText nameInput = new EditText(this);
		
		dialogbuilder
			.setTitle("Enter a route name:")
			.setView(nameInput)
			.setPositiveButton("Publish", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.w(TAG, nameInput.getText().toString());
	
					routeName = nameInput.getText().toString();
					if (routeName.length() == 0){
						Log.w(TAG+"E", "empty name");
						Toast.makeText(getApplicationContext(), "Name cannot be empty", Toast.LENGTH_LONG).show();
						
						return;
					}
					else if (last_point_location == null) {
						Log.w(TAG+"E", "empty list");
						Toast.makeText(getApplicationContext(), "You must add at least one point", Toast.LENGTH_LONG).show();
						
						return;
					}
//					else if (current_location.distanceTo(last_point_location) < 20) {
//						Log.w(TAG+"E", "not enough distance");
//						Toast.makeText(getApplicationContext(), "Too close to previous point", Toast.LENGTH_LONG).show();
//						
//						return;
//					}
					else if (current_location.getLatitude() == 0 && current_location.getLongitude() == 0) {
						Log.w(TAG+"E", "haven't gotten a location yet");
						Toast.makeText(getApplicationContext(), "Haven't updated location yet", Toast.LENGTH_LONG).show();
						
						return;
					}
									
					constructedRoute += Double.toString(currentLat) + "," + Double.toString(currentLng);
					constructedRoute += "|BREAK_P|FINISH!";

						
					post_asynctask = new TrailsPost();
		            
		            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		            		post_asynctask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
		            else
		            		post_asynctask.execute();
					
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					post_asynctask.cancel(true);					
				}
			})
			.show();
		
	}

	public class TrailsPost extends AsyncTask<String, Void, Integer> {

		protected void onPreExecute() {

		}
		
		@Override
		protected Integer doInBackground(String... params) {
			Log.e(TAG+"STARTASYNC", "doInBackground");
			
			if (routeName.length() == 0)
				return 1;
			
			HttpResponse response = null;
			HttpPost post = new HttpPost("http://trails-server.herokuapp.com/addRoute");

			if(isCancelled())
  	    			return 3;
  	    
	  	    try {
					MultipartEntityBuilder entity = MultipartEntityBuilder.create();
							
					entity.addPart("points_to_clues", new StringBody(constructedRoute));
					
	                entity.addPart("name", new StringBody(routeName));

					post.setEntity(entity.build());
					if(isCancelled())
						return 3;
		  	    
					response = new DefaultHttpClient().execute(post);
					
	    	    } catch (Exception e) {
		    	    	e.printStackTrace();
		    	    	Log.e(TAG, "error :(");
	    	    }
	    	    
	  	    	if(isCancelled())
	  	    		return 3;
	  	    
	    	    if (response != null) {
		    	    	try {	
		    	    		Log.e(TAG+"+E", "Printing response");
		    	    		Log.e(TAG+"+E", EntityUtils.toString(response.getEntity()));
			    	    	
		    	    		if (response.getStatusLine().getStatusCode() != 200)
			    	    		return 2;
	
				} catch (Exception e) {
					Log.e(TAG+"+E", "EXCEPSHUN");
					e.printStackTrace();
				}
	    	    }
	    	    Log.e(TAG+"+ENDASYNC", "done");
			return 0;
		}
		
		@Override
		protected void onPostExecute(Integer i) {

			if (i == 2) {
				//network error
				Log.w(TAG, "network error");
				Toast.makeText(getBaseContext(), "Network error", Toast.LENGTH_LONG).show();
				return;
			}
			else if (i == 1) {
				//no name given error, show an alert dialog
				Log.w(TAG, "name error");
				Toast.makeText(getBaseContext(), "Name error", Toast.LENGTH_LONG).show();
				return;
			}
			
			Log.w(TAG, "submit SUCCESS");
			Toast.makeText(getBaseContext(), "Published successfully!", Toast.LENGTH_LONG).show();
			finish();
		}
		
		protected void OnCancelled(Integer i) {

			Log.e(TAG+"CANCELLED", "cancelled");
			Toast.makeText(getBaseContext(), "Cancelled", Toast.LENGTH_LONG);
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		currentLat = location.getLatitude();
		currentLng = location.getLongitude();
		Log.e(TAG+"onLoc", "location changed");
		current_location.setLatitude(currentLat);
		current_location.setLongitude(currentLng);
		((TextView) findViewById(R.id.coords)).setText(currentLat + "," + currentLng);
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
