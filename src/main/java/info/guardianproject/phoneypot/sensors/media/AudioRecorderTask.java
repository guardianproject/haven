
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package info.guardianproject.phoneypot.sensors.media;


import android.content.ContentValues;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import info.guardianproject.phoneypot.PreferenceManager;

public class AudioRecorderTask extends Thread {
	
	/**
	 * Context used to retrieve shared preferences
	 */
	@SuppressWarnings("unused")
	private Context context;
	
	/**
	 * Shared preferences of the application
	 */
	private PreferenceManager prefs;
	
	/**
	 * Name of the audio file to store
	 */
	private String filename;

	/**
	 * Path of the audio file for this instance
	 */
	private String audioPath;

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
		this.prefs = new PreferenceManager(context);
		this.filename = prefs.getAudioPath();		
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

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        
        audioPath = Environment.getExternalStorageDirectory().getPath() +
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
			Thread.sleep(prefs.getAudioLength());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        recorder.stop();
        Log.i("AudioRecorderTask", "Stopped recording");
        recorder.release();
        recording = false;
        
        MicrophoneTaskFactory.restartSampling();

	}

	public String getAudioFilePath ()
	{
		return audioPath;
	}

}
