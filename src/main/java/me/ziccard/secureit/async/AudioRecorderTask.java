/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import me.ziccard.secureit.SecureItPreferences;
import me.ziccard.secureit.config.Remote;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class AudioRecorderTask extends Thread {
	
	/**
	 * Context used to retrieve shared preferences
	 */
	@SuppressWarnings("unused")
	private Context context;
	
	/**
	 * Shared preferences of the application
	 */
	private SecureItPreferences prefs;
	
	/**
	 * Name of the audio file to store
	 */
	private String filename;
	
	/**
	 * Id of the phone
	 */
	private String phoneId;

	/**
	 * Access token to upload the audio
	 */
	private String accessToken;
	
	/**
	 * True iff the thread is recording
	 */
	private boolean recording = false;
	
	/**
	 * Getter for recording data field
	 */
	public boolean isRecording() {
		return recording;
	}
	
	/**
	 * We make recorder protected in order to forse
	 * Factory usage
	 */
	protected AudioRecorderTask(Context context) {
		super();
		this.context = context;
		this.prefs = new SecureItPreferences(context);
		this.filename = prefs.getAudioPath();		
		this.phoneId = prefs.getPhoneId();
		this.accessToken = prefs.getAccessToken();
		Log.i("AudioRecorderTask", "Created recorder");
	}
	
	@Override
	public void run() {
		
        MicrophoneTaskFactory.pauseSampling();
        
        while (MicrophoneTaskFactory.isSampling()) {
        	try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		
		recording = true;
		final MediaRecorder recorder = new MediaRecorder();
		
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, filename);
        
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        
        String audioPath = Environment.getExternalStorageDirectory().getPath() +
        		filename + 
        		".m4a";

        recorder.setOutputFile(audioPath);
        try {
          recorder.prepare();
        } catch (Exception e){
            e.printStackTrace();
            return;
        }
        
        Log.i("AudioRecorderTask", "Start recording");
        recorder.start();
        try {
			Thread.sleep(prefs.getAudioLenght());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        recorder.stop();
        Log.i("AudioRecorderTask", "Stopped recording");
        recorder.release();
        recording = false;
        
        MicrophoneTaskFactory.restartSampling();
        
        /*
         * Uploading the audio 
         */
        Log.i("AudioRecorderTask", "Trying to upload");
        HttpClient httpclient = new DefaultHttpClient();
		HttpPost request = new HttpPost(
				Remote.HOST+
				Remote.PHONES+"/"+phoneId+
				Remote.UPLOAD_AUDIO);
		
		Log.i("AudioRecorderTask", 
				"URI: "+Remote.HOST+
				Remote.PHONES+"/"+phoneId+
				Remote.UPLOAD_AUDIO);
		/*
		 * Getting the audio from the file system
		 */
	    MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
	    File audio = new File(audioPath);
	    reqEntity.addPart("audio", new FileBody(audio, "audio/mp3"));	
		request.setEntity(reqEntity);
		
		/*
		 * Authentication token
		 */
		request.setHeader("access_token", accessToken);
		
		try {
			HttpResponse response = httpclient.execute(request);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
			    builder.append(line).append("\n");
			}
			
			Log.i("AudioRecorderTask", "Response:\n"+builder.toString());
			
			if (response.getStatusLine().getStatusCode() != 200) {
				Log.i("AudioRecorderTask", "Error uploading audio: "+audioPath);
				throw new HttpException();
			}
		} catch (Exception e) {
			Log.e("DataUploaderTask", "Error uploading audio: "+audioPath);
		}       
	}

}
