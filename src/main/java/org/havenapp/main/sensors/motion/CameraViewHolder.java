
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package org.havenapp.main.sensors.motion;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;
import android.view.Surface;

import com.google.android.cameraview.CameraView;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.service.MonitorService;
import org.jcodec.api.android.AndroidSequenceEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.silvaren.easyrs.tools.Nv21Image;

public class CameraViewHolder {

    /**
     * Object to retrieve and set shared preferences
     */
    private PreferenceManager prefs;

    private final static int PREVIEW_INTERVAL = 200;

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
    private boolean doingVideoProcessing = false;

	/**
	 * Handler used to update back the UI after motion detection
	 */
	private final Handler updateHandler = new Handler();

	/**
	 * Sensitivity of motion detection
	 */
	private int motionSensitivity = LuminanceMotionDetector.MOTION_MEDIUM;

    /**
     * holder of the CameraView and state of running
     */
    private CameraView cameraView = null;
    private boolean isCameraStarted = false;

	/**
	 * Messenger used to signal motion to the alert service
	 */
	private Messenger serviceMessenger = null;
	//private Camera camera;
	private Activity context;
	private MotionDetector task;

    AndroidSequenceEncoder encoder;
    private File videoFile;

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

	public CameraViewHolder(Activity context, CameraView cameraView) {
		//super(context);
		this.context = context;
		this.cameraView = cameraView;
        this.renderScript = RenderScript.create(context); // where context can be your activity, application, etc.

		prefs = new PreferenceManager(context);

        task = new MotionDetector(
                renderScript,
                updateHandler,
                motionSensitivity);

        task.addListener((sourceImage, detectedImage, rawBitmap, motionDetected) -> {

            for (MotionDetector.MotionListener listener : listeners)
                listener.onProcess(sourceImage,detectedImage,rawBitmap,motionDetected);

            if (motionDetected) {

                if (serviceMessenger != null) {
                    Message message = new Message();
                    message.what = EventTrigger.CAMERA;

                    try {

                        File fileImageDir = new File(Environment.getExternalStorageDirectory(), prefs.getDefaultMediaStoragePath());
                        fileImageDir.mkdirs();

                        String ts = new Date().getTime() + ".jpg";

                        File fileImage = new File(fileImageDir, "detected.original." + ts);
                        FileOutputStream stream = new FileOutputStream(fileImage);
                        rawBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                        stream.flush();
                        stream.close();
                        message.getData().putString("path", fileImage.getAbsolutePath());

                        //store the still match frame, even if doing video
                        serviceMessenger.send(message);

                        if (prefs.getVideoMonitoringActive() && (!doingVideoProcessing)) {
                            recordVideo();

                        }

                    } catch (Exception e) {
                        // Cannot happen
                        Log.e("CameraViewHolder", "error creating image", e);
                    }
                }
            }


        });
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
				task.setMotionSensitivity(motionSensitivity);
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
	public synchronized void startCamera() {


        updateCamera();

        cameraView.start();

        cameraView.setOnFrameListener((data, width, height, rotationDegrees) -> {

            long now = System.currentTimeMillis();
            if (now < CameraViewHolder.this.lastTimestamp + PREVIEW_INTERVAL)
                return;

            CameraViewHolder.this.lastTimestamp = now;

            if (!doingVideoProcessing) {

                Log.i("CameraViewHolder", "Processing new image");

                mDecodeThreadPool.execute(() -> processNewFrame(data, width, height, rotationDegrees));
            } else {
                mEncodeVideoThreadPool.execute(() -> recordNewFrame(data, width, height, rotationDegrees));
            }
        });


    }

    public void updateCamera ()
    {
        switch (prefs.getCamera()) {
            case PreferenceManager.FRONT:
                if (cameraView.getFacing() != CameraView.FACING_FRONT)
                    cameraView.setFacing(CameraView.FACING_FRONT);
                break;
            case PreferenceManager.BACK:
                if (cameraView.getFacing() != CameraView.FACING_BACK)
                    cameraView.setFacing(CameraView.FACING_BACK);
                break;
            default:
                //	camera = null;
                break;
        }
    }

    // A queue of Runnables
    private final BlockingQueue<Runnable> mDecodeWorkQueue = new LinkedBlockingQueue<Runnable>();

    // Creates a thread pool manager
    private ThreadPoolExecutor mDecodeThreadPool = new ThreadPoolExecutor(
            1,       // Initial pool size
            1,       // Max pool size
            10,
            TimeUnit.SECONDS,
            mDecodeWorkQueue);

    // A queue of Runnables
    private final BlockingQueue<Runnable> mEncodeVideoWorkQueue = new LinkedBlockingQueue<Runnable>();

    // Creates a thread pool manager
    private ThreadPoolExecutor mEncodeVideoThreadPool = new ThreadPoolExecutor(
            1,       // Initial pool size
            1,       // Max pool size
            10,
            TimeUnit.SECONDS,
            mEncodeVideoWorkQueue);


    private Matrix mtxVideoRotate;

    private void recordNewFrame (byte[] data, int width, int height, int rotationDegrees)
    {

        Bitmap bitmap = Nv21Image.nv21ToBitmap(renderScript, data, width, height);

        bitmap = Bitmap.createBitmap(bitmap,0,0,width,height,mtxVideoRotate,true);

        try {
            if (encoder != null)
             encoder.encodeImage(bitmap);

            bitmap.recycle();

        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    private void finishVideoEncoding ()
    {
        try {
            encoder.finish();

            if (serviceMessenger != null) {
                Message message = new Message();
                message.what = EventTrigger.CAMERA_VIDEO;
                message.getData().putString(MonitorService.KEY_PATH, videoFile.getAbsolutePath());
                try {
                    serviceMessenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private synchronized void processNewFrame (byte[] data, int width, int height, int rotationDegrees)
    {
        task.detect(
                lastPic,
                data,
                width,
                height,
                cameraView.getDefaultOrientation(),
                cameraView.getFacing());

        lastPic = data;

    }


	private synchronized boolean recordVideo() {

	    if (doingVideoProcessing)
	        return false;
        String ts1 = String.valueOf(new Date().getTime());
        File fileStoragePath = new File(Environment.getExternalStorageDirectory(),prefs.getDefaultMediaStoragePath());
        fileStoragePath.mkdirs();

        videoFile =  new File(fileStoragePath, ts1 + ".mp4");

        try {
            encoder = AndroidSequenceEncoder.createSequenceEncoder(videoFile,5);

        } catch (IOException e) {
            e.printStackTrace();
        }

        mtxVideoRotate = new Matrix();

        if (cameraView.getFacing() == CameraView.FACING_FRONT) {
            mtxVideoRotate.postRotate(-cameraView.getDefaultOrientation());
            mtxVideoRotate.postScale(-1, 1, cameraView.getWidth() / 2, cameraView.getHeight() / 2);
        }
        else
            mtxVideoRotate.postRotate(cameraView.getDefaultOrientation());

        doingVideoProcessing = true;

        int seconds = prefs.getMonitoringTime() * 1000;
        updateHandler.postDelayed(() -> {
           doingVideoProcessing = false;
            finishVideoEncoding();
        }, seconds);

        for (MotionDetector.MotionListener listener : listeners)
            listener.onProcess(null, null, null, false);

        return true;
    }


    public synchronized void stopCamera ()
    {
        if (cameraView != null) {
           cameraView.stop();
        }
    }

    public int getCameraFacing() {
        return cameraView.getFacing();
    }

    public void destroy ()
    {
        if (mConnection != null) {
            this.context.unbindService(mConnection);
            mConnection = null;
        }
        stopCamera();
    }

    public int getCorrectCameraOrientation(int facing, int orientation) {

        int rotation = context.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch(rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }

        int result;
        if(facing == CameraView.FACING_FRONT){
            result = (orientation + degrees) % 360;
            result = (360 - result) % 360;
        }else{
            result = (orientation - degrees + 360) % 360;
        }

        return result;
    }

    public boolean doingVideoProcessing ()
    {
        return doingVideoProcessing;
    }

}
