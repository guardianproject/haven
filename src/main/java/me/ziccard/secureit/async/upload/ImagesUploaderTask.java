/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import me.ziccard.secureit.SecureItPreferences;
import me.ziccard.secureit.config.Remote;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class ImagesUploaderTask extends AsyncTask<Void, Void, Void> {
	
	@SuppressWarnings("unused")
	private Context context;
	
	private SecureItPreferences prefs;

	public static final int WIFI_CONNECTIVITY = 0;

	public static final int MOBILE_CONNECTIVITY = 1;

	public static final int NO_CONNECTIVITY = 2;
	
	/**
	 * ID of the phone sending data
	 */
	private String phoneId = null;
	
	/**
	 * Access token of the phone sending data
	 */
	private String accessToken = null;

	/**
	* Private connectivity type
	*/
	private int connectivityType;

	/**
	 * Constructor
	 * @param phoneId
	 * @param accessToken
	 */
	public ImagesUploaderTask(Context context, int connectivityType) {
		this.context = context;
		prefs = new SecureItPreferences(context);
			
		this.phoneId = prefs.getPhoneId();
		this.accessToken = prefs.getAccessToken();
		this.connectivityType = connectivityType;
	}
	

	@Override
	protected Void doInBackground(Void... params) {
		HttpClient client = new DefaultHttpClient();
		
		Log.i("ImagesUploaderTask","Started");
		
		int imagesToUpload = 0;
		
		/*
		 * If we are using mobile connectivity we upload half of the images
		 * stored 
		 */
		if (connectivityType == WIFI_CONNECTIVITY) {
			imagesToUpload = prefs.getMaxImages();
		}
		if (connectivityType == MOBILE_CONNECTIVITY) {
			imagesToUpload = prefs.getMaxImages() / 2;
		}
		if (connectivityType == NO_CONNECTIVITY) {
			imagesToUpload = 0;
		}
		
		for (int imageCount = 0; imageCount < imagesToUpload; imageCount++) {
			
			String path = Environment.getExternalStorageDirectory().getPath() +
					prefs.getImagePath() + 
					imageCount  + 
					".jpg";
			
			HttpPost request = new HttpPost(
					Remote.HOST+
					Remote.PHONES+"/"+phoneId+
					Remote.UPLOAD_IMAGES);
			
			Log.i("ImagesUploaderTask", "Uploading image "+path);
			
			/*
			 * Get image from filesystem
			 */
			File image = new File(path);
			
			//Only if the image exists we upload it 
			if (image.exists()) {
				
				Log.i("ImagesUploaderTask", "Image exists");
				
				/*
				 * Getting the image from the file system
				 */
			    MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			    reqEntity.addPart("image", new FileBody(image, "image/jpeg"));	
				request.setEntity(reqEntity);
			
				/*
				 * Setting the access token
				 */
				request.setHeader("access_token", accessToken);
				
				try {
					HttpResponse response = client.execute(request);
					
					BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
					StringBuilder builder = new StringBuilder();
					for (String line = null; (line = reader.readLine()) != null;) {
					    builder.append(line).append("\n");
					}
					
					Log.i("ImagesRecorderTask", "Response:\n"+builder.toString());
					
					if (response.getStatusLine().getStatusCode() != 200) {
						throw new HttpException();
					}
				} catch (Exception e) {
					Log.e("ImageUploaderTask", "Error uploading image: "+path);
				}
			//otherwise no other image exists
			} else {
				return null;
			}
		}
		return null;
	}
}
