/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package org.havenapp.main.sensors.media;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.util.Log;

import org.havenapp.main.sensors.motion.IMotionDetector;
import org.havenapp.main.sensors.motion.LuminanceMotionDetector;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Task doing all image processing in backgrounds, 
 * has a collection of listeners to notify in after having processed
 * the image
 * @author marco
 *
 */
public class MotionAsyncTask extends Thread {
	
	// Input data
	
	private List<MotionListener> listeners = new ArrayList<>();
	private byte[] rawOldPic;
	private byte[] rawNewPic;
	private int width;
	private int height;
	private Handler handler;
	private int motionSensitivity;
	
	// Output data
	
	private Bitmap lastBitmap;
	private Bitmap newBitmap;
	private Bitmap rawBitmap;
	private boolean hasChanged;

	private IMotionDetector detector;
	
	public interface MotionListener {
		public void onProcess(Bitmap oldBitmap,
				Bitmap newBitmap,
							  Bitmap rawBitmap,
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

	public void setMotionSensitivity (int motionSensitivity)
	{
		this.motionSensitivity = motionSensitivity;
		detector.setThreshold(motionSensitivity);
	}

	@Override
	public void run() {
		int[] newPicLuma = ImageCodec.N21toLuma(rawNewPic, width, height);
		if (rawOldPic == null) {
			newBitmap = ImageCodec.lumaToBitmapGreyscale(newPicLuma, width, height);
			lastBitmap = newBitmap;
		} else {
		    int[] oldPicLuma = ImageCodec.N21toLuma(rawOldPic, width, height);
			detector = new LuminanceMotionDetector();
			detector.setThreshold(motionSensitivity);
			List<Integer> changedPixels = 
					detector.detectMotion(oldPicLuma, newPicLuma, width, height);
			hasChanged = false;
	
			int[] newPic = ImageCodec.lumaToGreyscale(newPicLuma, width, height);
			if (changedPixels != null) {
				hasChanged = true;
				for (int changedPixel : changedPixels) {
					newPic[changedPixel] = Color.YELLOW;
				}
			}

			lastBitmap = ImageCodec.lumaToBitmapGreyscale(oldPicLuma, width, height);
			newBitmap = Bitmap.createBitmap(newPic, width, height, Bitmap.Config.RGB_565);

			if (hasChanged) {
				YuvImage image = new YuvImage(rawNewPic, ImageFormat.NV21, width, height, null);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				image.compressToJpeg(
						new Rect(0, 0, image.getWidth(), image.getHeight()), 90,
						baos);

				byte[] imageBytes = baos.toByteArray();
				rawBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
				// Setting post rotate to 90
				Matrix mtx = new Matrix();
				mtx.postRotate(-90);
				// Rotating Bitmap
				rawBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, width, height, mtx, true);
			}
			else
			{
				rawBitmap = null;
			}
		}
		
		Log.i("MotionAsyncTask", "Finished processing, sending results");
		handler.post(new Runnable() {
			
			public void run() {
				for (MotionListener listener : listeners) {
					Log.i("MotionAsyncTask", "Updating back view");
					listener.onProcess(
							lastBitmap,
							newBitmap,
							rawBitmap,
							hasChanged);
				}
				
			}
		});
	}


}
