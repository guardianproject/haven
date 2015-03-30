/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async;

import android.content.Context;

public class MicrophoneTaskFactory {
	
	public static class RecordLimitExceeded extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7030672869928993643L;
		
	}
	
	private static AudioRecorderTask recorderTask;
	
	private static MicSamplerTask samplerTask; 
	
	public static AudioRecorderTask makeRecorder(Context context) throws RecordLimitExceeded {
		if (recorderTask != null && recorderTask.isRecording()) 
			throw new RecordLimitExceeded();
		recorderTask = new AudioRecorderTask(context);
		return recorderTask;
	}
	
	public static MicSamplerTask makeSampler(Context context) throws RecordLimitExceeded {
		if ((recorderTask != null && recorderTask.isRecording()) || (samplerTask != null && !samplerTask.isCancelled())) 
			throw new RecordLimitExceeded();
		samplerTask = new MicSamplerTask();
		return samplerTask;
	}
	
	public static void pauseSampling() {
		if (samplerTask != null) {
			samplerTask.pause();
		}
	}
	
	public static void restartSampling() {
		if (samplerTask != null) {
			samplerTask.restart();
		}
	}
	
	public static boolean isSampling() {
		return samplerTask != null && samplerTask.isSampling();
	}
	
	public static boolean isRecording() {
		return recorderTask != null && recorderTask.isRecording();
	}

}
