/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async.upload;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import me.ziccard.secureit.config.Remote;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.os.AsyncTask;
import android.util.Log;

public class DelegatedPositionUploaderTask extends AsyncTask<Void, Void, Void> {
	
	/**
	 * Latitude of the last position to be uploaded
	 */
	protected double lat;
	
	/**
	 * Longitude of the last position to be uploaded
	 */
	protected double lng;
	
	/**
	 * ID of the phone sending data
	 */
	private String phoneId = null;
	
	/**
	 * Access token of the phone sending data
	 */
	private String accessKey = null;

	/**
	 * Constructor
	 * @param context
	 * @param lat
	 * @param lng
	 */
	public DelegatedPositionUploaderTask(String phoneId, double lat, double lng, String accessKey) {

		this.lat = lat;
		this.lng = lng;
		this.phoneId = phoneId;
		this.accessKey = accessKey;
			
	}
		
	@Override
	protected Void doInBackground(Void... params) {
		
		while(true) {
			
			Log.i("DelegatedPositionUploaderTask", "Started");
					
			HttpClient client = new DefaultHttpClient();
			
			/*
			 * Send the last
			 * detected position to /phone/phoneId/position [POST]
			 */
			HttpPost request = new HttpPost(
						Remote.HOST+
						Remote.PHONES+"/"+phoneId+
						Remote.DELEGATED_UPLOAD_POSITION);
				
			/*
			 * Adding latitude and longitude
			 */
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		    nameValuePairs.add(new BasicNameValuePair("lat", ""+lat));
		    nameValuePairs.add(new BasicNameValuePair("long", ""+lng));
		    nameValuePairs.add(new BasicNameValuePair("access_key", accessKey));
		    try {
		    	request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		        				

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

		    } catch (Exception e) {
		    	Log.e("DelegatedPositionUploaderTask", "Error uploading delegated location");
		    }
		}
	}
}