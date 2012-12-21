package me.ziccard.secureit.async;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import me.ziccard.secureit.config.Remote;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.os.AsyncTask;
import android.util.Log;

public class DataUploaderTask extends AsyncTask<Void, Void, Void> {

	private static final int WIFI_CONNECTIVITY = 0;

	private static final int MOBILE_CONNECTIVITY = 1;

	private static final int NO_CONNECTIVITY = 2;
	
	/**
	 * Paths to images to be uploaded
	 */
	private List<String> imagePaths = null;
	
	/**
	 * Path to audio to be uploaded
	 */
	private String audioPath = null;
	
	/**
	 * Latitude of the last position to be uploaded
	 */
	private Double lat = null;
	
	/**
	 * Longitude of the last position to be uploaded
	 */
	private Double lon = null;
	
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
	public DataUploaderTask(String phoneId, String accessToken, int connectivityType) {
		this.phoneId = phoneId;
		this.accessToken = accessToken;
		this.connectivityType = connectivityType;
	}
	
	public void setImagesPath(List<String> paths) {
		this.imagePaths = paths;
	}
	
	public void setAudioPath(String path) {
		this.audioPath = path;
	}

	@Override
	protected Void doInBackground(Void... params) {
		HttpClient client = new DefaultHttpClient();
		/*
		 * If imagePaths != null send all the images
		 * to /phone/phoneId/images [POST]
		 */
		if (imagePaths != null) {
			for (String path : imagePaths) {
				HttpPost request = new HttpPost(
						Remote.HOST+
						Remote.PHONES+"/"+phoneId+
						Remote.UPLOAD_IMAGES);
				/*
				 * Getting the image from the file system
				 */
				File image = new File(path);
				FileEntity entity = new FileEntity(image, "image/jpeg");
				request.setEntity(entity);
				
				/*
				 * Authentication token
				 */
				request.setHeader("access_token", accessToken);
				
				try {
					HttpResponse response = client.execute(request);
					if (response.getStatusLine().getStatusCode() != 200) {
						throw new HttpException();
					}
				} catch (Exception e) {
					Log.e("DataUploaderTask", "Error uploading image: "+path);
				} 
			}
		}
		/*
		 * If audioPath != null send the track
		 * to /phone/phoneId/audio [POST]
		 */
		if (audioPath != null) {
			HttpPost request = new HttpPost(
					Remote.HOST+
					Remote.PHONES+"/"+phoneId+
					Remote.UPLOAD_AUDIO);
			/*
			 * Getting the audio from the file system
			 */
			File audio = new File(audioPath);
			FileEntity entity = new FileEntity(audio, "audio/mp3");
			request.setEntity(entity);
			
			/*
			 * Authentication token
			 */
			request.setHeader("access_token", accessToken);
			
			try {
				HttpResponse response = client.execute(request);
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new HttpException();
				}
			} catch (Exception e) {
				Log.e("DataUploaderTask", "Error uploading audio: "+audioPath);
			} 
		}
		/*
		 * If lat != null and lon != null send the last
		 * detected position to /phone/phoneId/position [POST]
		 */
		if (lat != null && lon != null) {
			HttpPost request = new HttpPost(
					Remote.HOST+
					Remote.PHONES+"/"+phoneId+
					Remote.UPLOAD_POSITION);
			
	        /*
	         * Adding latitude and longitude
	         */
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("lat", 
	        		lat.toString()));
	        nameValuePairs.add(new BasicNameValuePair("long", 
	        		lon.toString()));
	        try {
	        	request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	        	
	        	/*
				 * Authentication token
				 */
				request.setHeader("access_token", accessToken);
				
				HttpResponse response = client.execute(request);
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new HttpException();
				}
	        } catch (Exception e) {
	        	Log.e("DataUploaderTask", "Error uploading location");
	        }
		}
		return null;
	}
	

}
