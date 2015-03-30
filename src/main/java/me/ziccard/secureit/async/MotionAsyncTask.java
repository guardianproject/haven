/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async;

import java.util.ArrayList;
import java.util.List;

import me.ziccard.secureit.codec.ImageCodec;
import me.ziccard.secureit.motiondetection.IMotionDetector;
import me.ziccard.secureit.motiondetection.LuminanceMotionDetector;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

/**
 * Task doing all image processing in backgrounds, 
 * has a collection of listeners to notify in after having processed
 * the image
 * @author marco
 *
 */
public class MotionAsyncTask extends Thread {
	
	// Input data
	
	private List<MotionListener> listeners = new ArrayList<MotionListener>();
	private byte[] rawOldPic;
	private byte[] rawNewPic;
	private int width;
	private int height;
	private Handler handler;
	private int motionSensitivity;
	
	// Output data
	
	private Bitmap lastBitmap;
	private Bitmap newBitmap;
	private boolean hasChanged;
	
	public interface MotionListener {
		public void onProcess(Bitmap oldBitmap,
				Bitmap newBitmap,
				boolean motionDetected);
	}
	
	public void addListener(MotionListener listener) {
		listeners.add(listener);
	}
	
	public MotionAsyncTask(
			byte[] rawOldPic, 
			byte[] rawNewPic, 
			int width, 
			int height,
			Handler updateHandler,
			int motionSensitivity) {
		this.rawOldPic = rawOldPic;
		this.rawNewPic = rawNewPic;
		this.width = width;
		this.height = height;
		this.handler = updateHandler;
		this.motionSensitivity = motionSensitivity;
		
	}

	@Override
	public void run() {
		int[] newPicLuma = ImageCodec.N21toLuma(rawNewPic, width, height);
		if (rawOldPic == null) {
			newBitmap = ImageCodec.lumaToBitmapGreyscale(newPicLuma, width, height);
			lastBitmap = newBitmap;
		} else {
		    int[] oldPicLuma = ImageCodec.N21toLuma(rawOldPic, width, height);
			IMotionDetector detector = new LuminanceMotionDetector();
			detector.setThreshold(motionSensitivity);
			List<Integer> changedPixels = 
					detector.detectMotion(oldPicLuma, newPicLuma, width, height);
			hasChanged = false;
	
			int[] newPic = ImageCodec.lumaToGreyscale(newPicLuma, width, height);
			if (changedPixels != null) {
				hasChanged = true;
				for (int changedPixel : changedPixels) {
					newPic[changedPixel] = Color.RED;
				}
			}
			
			lastBitmap = ImageCodec.lumaToBitmapGreyscale(oldPicLuma, width, height);
			newBitmap = Bitmap.createBitmap(newPic, width, height, Bitmap.Config.RGB_565);
		}
		
		Log.i("MotionAsyncTask", "Finished processing, sending results");
		handler.post(new Runnable() {
			
			public void run() {
				for (MotionListener listener : listeners) {
					Log.i("MotionAsyncTask", "Updating back view");
					listener.onProcess(
							lastBitmap,
							newBitmap,
							hasChanged);
				}
				
			}
		});
	}
}
