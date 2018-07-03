
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package org.havenapp.main.sensors.motion;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import com.google.android.cameraview.CameraView;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.service.MonitorService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Preview {

    /**
     * Object to retrieve and set shared preferences
     */
    private PreferenceManager prefs;
    private int cameraFacing = 0;

    private final static int PREVIEW_INTERVAL = 500;

    private List<MotionDetector.MotionListener> listeners = new ArrayList<>();

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
    private boolean doingProcessing, doingVideoProcessing = false;

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
	private CameraView cameraView = null;
	private Messenger serviceMessenger = null;
	//private Camera camera;
	private Context context;
	private MotionDetector task;
    private String videoFile;

    //for managing bitmap processing
    private RenderScript renderScript;

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

	public Preview (Context context, CameraView cameraView) {
		//super(context);
		this.context = context;
		this.cameraView = cameraView;
        this.renderScript = RenderScript.create(context); // where context can be your activity, application, etc.

		prefs = new PreferenceManager(context);
		
		motionSensitivity = prefs.getCameraSensitivity();

		initCamera();
	/*
		 * We bind to the alert service
		 */
        this.context.bindService(new Intent(context,
                MonitorService.class), mConnection, Context.BIND_ABOVE_CLIENT);
	}

	public void setMotionSensitivity (int
				motionSensitivity )
				{
				this.
				motionSensitivity = motionSensitivity;
	}
	
	public void addListener(MotionDetector.MotionListener listener) {
		listeners.add(listener);
	}
	

	/**
	 * Called on the creation of the surface:
	 * setting camera parameters to lower possible resolution
	 * (preferred is 640x480)
	 * in order to minimize CPU usage
	 */
	public void initCamera() {


	    //if (cameraView != null)
	      //  stopCamera();

        cameraView.start();

		/*
		 *  The Surface has been created, acquire the camera and tell it where
		 *  to draw.
		 *  If the selected camera is the front one we open it
		 */
		switch (prefs.getCamera()) {
			case PreferenceManager.FRONT:
				Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
				int cameraCount = Camera.getNumberOfCameras();
				for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
					Camera.getCameraInfo(camIdx, cameraInfo);
					if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
						try {
							//camera = Camera.open(camIdx);
							cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                            cameraView.setFacing(CameraView.FACING_FRONT);

                        } catch (RuntimeException e) {
							Log.e("Preview", "Camera failed to open: " + e.getLocalizedMessage());
						}
					}
				}
				break;
			case PreferenceManager.BACK:

				//camera = Camera.open();
                cameraView.setFacing(CameraView.FACING_BACK);
				cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
				break;
			default:
			//	camera = null;
				break;
		}


        cameraView.setOnFrameListener((data, width, height, rotationDegrees) -> {
         //   processNewFrame(data, width, height);
            long now = System.currentTimeMillis();
            if (now < Preview.this.lastTimestamp + PREVIEW_INTERVAL)
                return;

            Log.i("Preview", "Processing new image");
            Preview.this.lastTimestamp = now;

            mDecodeThreadPool.execute(() -> processNewFrame(data, width, height));
        });

    }

    // A queue of Runnables
    private final BlockingQueue<Runnable> mDecodeWorkQueue = new LinkedBlockingQueue<Runnable>();

    // Creates a thread pool manager
    ThreadPoolExecutor mDecodeThreadPool = new ThreadPoolExecutor(
            1,       // Initial pool size
            1,       // Max pool size
            10,
            TimeUnit.SECONDS,
            mDecodeWorkQueue);


    private void processNewFrame (byte[] data, int width, int height)
    {


        task = new MotionDetector(
                renderScript,
                lastPic,
                data,
                width,
                height,
                updateHandler,
                motionSensitivity);

        task.addListener((sourceImage, detectedImage, rawBitmap, motionDetected) -> {

            if (motionDetected) {
                Log.i("MotionListener", "Motion detected");

                for (MotionDetector.MotionListener listener : listeners)
                    listener.onProcess(sourceImage,detectedImage,rawBitmap,motionDetected);

                if (serviceMessenger != null) {
                    Message message = new Message();
                    message.what = EventTrigger.CAMERA;

                    try {

                        File fileImageDir = new File(Environment.getExternalStorageDirectory(), prefs.getImagePath());
                        fileImageDir.mkdirs();

                        String ts = new Date().getTime() + ".jpg";

                        File fileImage = new File(fileImageDir, "detected.original." + ts);
                        FileOutputStream stream = new FileOutputStream(fileImage);
                        rawBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                        stream.flush();
                        stream.close();
                        message.getData().putString("path", fileImage.getAbsolutePath());

                     //   sourceImage.recycle();
                      //  detectedImage.recycle();

                        //store the still match frame, even if doing video
                        serviceMessenger.send(message);

                        if (prefs.getVideoMonitoringActive() && (!doingVideoProcessing)) {
                            new Thread ()
                            {
                                public void run ()
                                {
                                    //   camera.stopPreview();
                                    //  record(camera, serviceMessenger);

                                }
                            }.start();
                        }

                    } catch (Exception e) {
                        // Cannot happen
                        Log.e("Preview", "error creating image", e);
                    }
                }
            }

            Log.i("MotionListener", "Allowing further processing");

        });

        task.detect();
        lastPic = data;

    }

    /**
	private synchronized boolean record(Camera cam, Messenger messenger) {

	    if (mediaRecorder != null && doingVideoProcessing)
	        return false;

        String ts1 = String.valueOf(new Date().getTime());
        videoFile = Environment.getExternalStorageDirectory() + File.separator + prefs.getImagePath() + File.separator + ts1 + ".mp4";
        int seconds = prefs.getMonitoringTime() * 1000;
        MediaRecorderTask mediaRecorderTask = new MediaRecorderTask(cam, videoFile, seconds, mHolder);
        mediaRecorder = mediaRecorderTask.getPreparedMediaRecorder();


        doingVideoProcessing = true;
        mediaRecorder.start();
        updateHandler.postDelayed(() -> {
            if (messenger != null) {
                Message message = new Message();
                message.what = EventTrigger.CAMERA_VIDEO;
                message.getData().putString("path", videoFile);
                try {
                    messenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                doingVideoProcessing = false;
            }
        }, seconds);

        return true;
    }**/


    public void stopCamera ()
    {
        if (cameraView != null) {
            // Surface will be destroyed when we return, so stop the preview.
            // Because the CameraDevice object is not a shared resource, it's very
            // important to release it when the activity is paused.
            this.context.unbindService(mConnection);

           cameraView.stop();
        }
    }

    public int getCameraFacing() {
        return cameraView.getFacing();
    }
}
