/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.ziccard.secureit.async.MotionAsyncTask;
import me.ziccard.secureit.async.MotionAsyncTask.MotionListener;
import me.ziccard.secureit.motiondetection.LuminanceMotionDetector;
import me.ziccard.secureit.service.UploadService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {
	
	/**
	 * Object to retrieve and set shared preferences
	 */
	private SecureItPreferences prefs;
	private int cameraFacing = 0;
	
	private List<MotionListener> listeners = new ArrayList<MotionListener>();
	
	/**
	 * Timestamp of the last picture processed
	 */
	private long lastTimestamp;
	/**
	 * Last picture processed
	 */
	private byte[] lastPic;
	/**
	 * True IFF there's an async task processing images
	 */
	private boolean doingProcessing;

	/**
	 * Handler used to update back the UI after motion detection
	 */
	private final Handler updateHandler = new Handler();
	
	/**
	 * Last frame captured
	 */
	private int imageCount = 0;
	
	/**
	 * Sensitivity of motion detection
	 */
	private int motionSensitivity = LuminanceMotionDetector.MOTION_MEDIUM;
	
	/**
	 * Messenger used to signal motion to the alert service
	 */
	private Messenger serviceMessenger = null;
	
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	Log.i("CameraFragment", "SERVICE CONNECTED");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            serviceMessenger = new Messenger(service);
        }
        
        public void onServiceDisconnected(ComponentName arg0) {
        	Log.i("CameraFragment", "SERVICE DISCONNECTED");
            serviceMessenger = null;
        }
    };
	
	
	SurfaceHolder mHolder;
	public Camera camera;
	private Context context;

	public Preview (Context context) {
		super(context);
		this.context = context;
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		prefs = new SecureItPreferences(context);
		
		/*
		 * Set sensitivity value
		 */
		if (prefs.getCameraSensitivity().equals("Medium")) {
			motionSensitivity = LuminanceMotionDetector.MOTION_MEDIUM;
			Log.i("CameraFragment", "Sensitivity set to Medium");
		} else if (prefs.getCameraSensitivity().equals("Low")) {
			motionSensitivity = LuminanceMotionDetector.MOTION_LOW;
			Log.i("CameraFragment", "Sensitivity set to Low");
		} else {
			motionSensitivity = LuminanceMotionDetector.MOTION_HIGH;
			Log.i("CameraFragment", "Sensitivity set to High");
		}
	}
	
	public void addListener(MotionListener listener) {
		listeners.add(listener);
	}
	

	/**
	 * Called on the creation of the surface:
	 * setting camera parameters to lower possible resolution
	 * (preferred is 640x480)
	 * in order to minimize CPU usage
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		
		/*
		 * We bind to the alert service
		 */
		context.bindService(new Intent(context, 
				UploadService.class), mConnection, Context.BIND_ABOVE_CLIENT);
		
		/*
		 *  The Surface has been created, acquire the camera and tell it where
		 *  to draw.
		 *  If the selected camera is the front one we open it
		 */
		if (prefs.getCamera().equals("Front")) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			int cameraCount = Camera.getNumberOfCameras();
			for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
				Camera.getCameraInfo( camIdx, cameraInfo );
			    if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT  ) {
			    	try {
			    		camera = Camera.open( camIdx );
			    		cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
			        } catch (RuntimeException e) {
			        	Log.e("Preview", "Camera failed to open: " + e.getLocalizedMessage());
			        }
			    }
			}
		} else {
			camera = Camera.open();
			cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
		}
		
		final Camera.Parameters parameters = camera.getParameters();
		List<Size> sizes = parameters.getSupportedPictureSizes();
		int w = 640;
		int h = 480;
		for (Size s : sizes) {
			Log.i("SurfaceView", "width: "+s.width+" height: "+s.height);
			if (s.width <= 640) {
				w = s.width;
				h = s.height;
				Log.i("SurfaceView", "selected width: "+w+" selected height: "+h);
				break;
			}
		}
		
		parameters.setPictureSize(w, h);

		/*
		 * If the flash is needed
		 */
		if (prefs.getFlashActivation()) {
			Log.i("Preview", "Flash activated");
			parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
		}
		
		camera.setParameters(parameters);
		
		try {
						
			camera.setPreviewDisplay(mHolder);
			
			camera.setPreviewCallback(new PreviewCallback() {
				
				public void onPreviewFrame(byte[] data, Camera cam) {
					
					final Camera.Size size = cam.getParameters().getPreviewSize();
					if (size == null) return;
					long now = System.currentTimeMillis();
					if (now < Preview.this.lastTimestamp + 1000)
						return;
					if (!doingProcessing) {
						
						/**
						 * Before processing the frame we save it
						 * to the SDCARD
						 */
					    try {
					        YuvImage image = new YuvImage(data, parameters.getPreviewFormat(), 
					                size.width, size.height, null);
					        
					        imageCount = (imageCount + 1)%(prefs.getMaxImages());
					        
					        File file = new File(
					        		Environment.getExternalStorageDirectory().getPath() +
					        		prefs.getImagePath() +
					        		imageCount +
					        		".jpg");
					        
					        FileOutputStream filecon = new FileOutputStream(file); 
					        
					        image.compressToJpeg( 
					                new Rect(0, 0, image.getWidth(), image.getHeight()), 90, 
					                filecon); 
					        
					    } catch (FileNotFoundException e) { 
					        Toast toast = Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG); 
					        toast.show(); 
					    } 
						
						Log.i("Preview", "Processing new image");
						Preview.this.lastTimestamp = now;
						MotionAsyncTask task = new MotionAsyncTask(
								lastPic,
								data,
								size.width,
								size.height,
								updateHandler,
								motionSensitivity);
						for (MotionListener listener : listeners) {
							Log.i("Preview", "Added listener");
							task.addListener(listener);
						}
						doingProcessing = true;
						task.addListener(new MotionListener() {
							
							public void onProcess(Bitmap oldBitmap, Bitmap newBitmap,
									boolean motionDetected) {
								
								if (motionDetected) {
									Log.i("MotionListener", "Motion detected");
									if (serviceMessenger!=null) {
										Message message = new Message();
										message.what = UploadService.CAMERA_MESSAGE;
										try {
											serviceMessenger.send(message);
										} catch (RemoteException e) {
											// Cannot happen
										}
									}
								}
								Log.i("MotionListener", "Allowing further processing");
								doingProcessing = false;								
							}
						});
						task.start();
						lastPic = data;
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		context.unbindService(mConnection);
		camera.setPreviewCallback(null);
		camera.stopPreview();
		camera.release();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(w, h);

		int degree = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		int displayOrientation = 0;
		switch (degree) {
		case Surface.ROTATION_0  : displayOrientation = 90; break;
		case Surface.ROTATION_90 : displayOrientation = 0; break;
		case Surface.ROTATION_180: displayOrientation = 0; break;
		case Surface.ROTATION_270: displayOrientation = 180; break;
		}
        camera.setDisplayOrientation(displayOrientation);
        
		//camera.setParameters(parameters);
		camera.startPreview();
	}
	
	public int getCameraFacing() {
	  return this.cameraFacing;
	}
}