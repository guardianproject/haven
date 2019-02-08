/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package org.havenapp.main.sensors.motion;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;

import org.havenapp.main.sensors.media.ImageCodec;

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
public class MotionDetector {
	
	// Input data
	
	private List<MotionListener> listeners = new ArrayList<>();
	private Handler handler;
	private int motionSensitivity;
	// Output data

	private boolean hasChanged;

	private IMotionDetector detector;

	//private RenderScript renderScript;

	private int detectColor = Color.YELLOW;

	public interface MotionListener {
		public void onProcess(Bitmap oldBitmap,
				Bitmap newBitmap,
							  Bitmap rawBitmap,
				boolean motionDetected);
	}
	
	public void addListener(MotionListener listener) {
		listeners.add(listener);
	}
	
	public MotionDetector(

			Handler updateHandler,
			int motionSensitivity) {
	   // this.renderScript = renderScript;
		this.handler = updateHandler;
		this.motionSensitivity = motionSensitivity;
        detector = new LuminanceMotionDetector();

    }

    public void setDetectColor (int detectColor)
    {
        this.detectColor = detectColor;
    }

	public void setMotionSensitivity (int motionSensitivity)
	{
		this.motionSensitivity = motionSensitivity;
		detector.setThreshold(motionSensitivity);
	}

	public void detect(byte[] rawOldPic,
                       byte[] rawNewPic,
                       int width,
                       int height,
                       int rotationDegrees,
                       boolean facingFront) {

		int[] newPicLuma = ImageCodec.N21toLuma(rawNewPic, width, height);
		if (rawOldPic != null) {

		    int[] oldPicLuma = ImageCodec.N21toLuma(rawOldPic, width, height);
			detector.setThreshold(motionSensitivity);
			List<Integer> changedPixels =
					detector.detectMotion(oldPicLuma, newPicLuma, width, height);
			hasChanged = false;

			int[] newPic = ImageCodec.lumaToGreyscale(newPicLuma, width, height);

			if (changedPixels != null) {
				hasChanged = true;

            }


			if (hasChanged) {


                Bitmap lastBitmap = ImageCodec.lumaToBitmapGreyscale(oldPicLuma, width, height);

                for (int i = 0; i < newPic.length; i++)
                    newPic[i] = Color.TRANSPARENT;

                for (int changedPixel : changedPixels) {
                    newPic[changedPixel] = detectColor;
                }


                Matrix mtx = new Matrix();

                if (facingFront) {
                    mtx.postRotate(-rotationDegrees);
                    mtx.postScale(-1, 1, width / 2, height / 2);
               }
               else
                  mtx.postRotate(rotationDegrees);


                Bitmap newBitmap
                        = Bitmap.createBitmap(Bitmap.createBitmap(newPic, width, height, Bitmap.Config.ARGB_4444), 0, 0, width, height, mtx, true);

                Bitmap rawBitmap = convertImage(rawNewPic,width,height);
						//Bitmap.createBitmap(Nv21Image.nv21ToBitmap(renderScript, rawNewPic, width, height),0,0,width,height,mtx,true);

                handler.post(() -> {
                    for (MotionListener listener : listeners) {
                        listener.onProcess(
                                lastBitmap,
                                newBitmap,
                                rawBitmap,
                                hasChanged);
                    }

                });
			}
			else
            {
                //nothing changed
                handler.post(() -> {
                    for (MotionListener listener : listeners) {
                        listener.onProcess(
                                null,
                                null,
                                null,
                                hasChanged);
                    }

                });
            }

		}


	}

	public static Bitmap convertImage (byte[] nv21bytearray, int width, int height)
	{
		YuvImage yuvImage = new YuvImage(nv21bytearray, ImageFormat.NV21, width, height, null);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, os);
		byte[] jpegByteArray = os.toByteArray();
		Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
		return bitmap;
	}


}
