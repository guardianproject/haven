/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async.upload;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import me.ziccard.secureit.SecureItPreferences;
import me.ziccard.secureit.config.Remote;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class PeriodicPositionUploaderTask extends AsyncTask<Void, Void, Void> implements LocationListener {
	
	private static final double INVALID_VALUE = Double.MAX_VALUE;
	
	/**
	 * In case 3G and WIFI are turned off we start the upload
	 * using bluetooth
	 */
	private BluetoothPeriodicPositionUploaderTask bluetoothTask;

	/**
	 * Context to retrieve shared preferences
	 */
	private Context context;
	
	/**
	 * Boolean true iff last thread iterations position has been sent
	 */
	private boolean dataSent = false;
	
	private SecureItPreferences prefs;
	
	/**
	 * Latitude of the last position to be uploaded
	 */
	protected double lat = INVALID_VALUE;
	
	/**
	 * Longitude of the last position to be uploaded
	 */
	protected double lng = INVALID_VALUE;
	
	/**
	 * Manager to retrieve location
	 */
	private LocationManager locationManager = null;
	
	/**
	 * ID of the phone sending data
	 */
	private String phoneId = null;
	
	/**
	 * Access token of the phone sending data
	 */
	private String accessToken = null;

	/**
	 * Constructor
	 * @param context
	 * @param lat
	 * @param lng
	 */
	public PeriodicPositionUploaderTask(Context context) {
		this.context = context;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER))
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER))
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		
		prefs = new SecureItPreferences(context);
			
		this.phoneId = prefs.getPhoneId();
		this.accessToken = prefs.getAccessToken();
	}
	
	@Override
	protected void onCancelled () {
		if (bluetoothTask != null) bluetoothTask.cancel(true);
		else locationManager.removeUpdates(this);		
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		
		while(true) {
			
			Log.i("PeriodicPositionUploaderTask", "Started");
			
			if (isCancelled()) {
				return null;
			}
		
			HttpClient client = new DefaultHttpClient();
			
			/*
			 * Send the last
			 * detected position to /phone/phoneId/position [POST]
			 */
			HttpPost request = new HttpPost(
						Remote.HOST+
						Remote.PHONES+"/"+phoneId+
						Remote.UPLOAD_POSITION);
				
			/*
			 * Adding latitude and longitude
			 */
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		    nameValuePairs.add(new BasicNameValuePair("lat", ""+lat));
		    nameValuePairs.add(new BasicNameValuePair("long", ""+lng));
		    try {
		    	request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		        	
		    	/*
		    	 * Access token
		    	 */
		    	request.setHeader("access_token", accessToken);
				
		    	/*
		    	 * We send position only if Gps/Network fixed
		    	 */
		    	
		    	if (lat != Double.MAX_VALUE && lng != Double.MAX_VALUE) {
		    		dataSent = true;
					HttpResponse response = client.execute(request);
					
					BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
					StringBuilder builder = new StringBuilder();
					for (String line = null; (line = reader.readLine()) != null;) {
					    builder.append(line).append("\n");
					}
					
					Log.i("PeriodicPositionUploaderTask", "Response:\n"+builder.toString());
					
					if (response.getStatusLine().getStatusCode() != 200) {
						throw new HttpException();
					}
		    	} else {
		    		Log.i("PeriodicPositionUploaderTask", "We have no position data yet");
		    		dataSent = false;
		    	}
		    } catch (Exception e) {
		    	Log.e("DataUploaderTask", "Error uploading location");
		    	/*
		    	 * We check if we still have connectivity, if this is not true
		    	 * we start BluetoothPeriodicPositionUploader
		    	 */
		    	ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
				boolean isConnected = activeNetwork.isConnectedOrConnecting();
				if (!isConnected) {
					bluetoothTask = new BluetoothPeriodicPositionUploaderTask(context);
					bluetoothTask.execute();
					locationManager.removeUpdates(this);
					return null;
				}
		    }
		 
		    try {
		    	if (dataSent) 	Thread.sleep(3600*1000);
		    	else			Thread.sleep(60*1000);
			} catch (InterruptedException e) {
				return null;
			}
		}
	}

	public void onLocationChanged(Location location) {
		lat = location.getLatitude();
		lng = location.getLongitude();
	}

	public void onProviderDisabled(String provider) {
		// Nothing interesting to do
		
	}

	public void onProviderEnabled(String provider) {
		// Nothing interesting to do
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Nothing interesting to do
		
	}
}