
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package org.havenapp.main.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.size.SizeSelector;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.Utils;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.sensors.motion.LuminanceMotionDetector;
import org.havenapp.main.sensors.motion.MotionDetector;
import org.havenapp.main.service.MonitorService;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CameraViewHolder {

    /**
     * Object to retrieve and set shared preferences
     */
    private PreferenceManager prefs;

    private final static int DETECTION_INTERVAL_MS = 200;
    private final static int MAX_CAMERA_WIDTH = 800;

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
	private MotionDetector motionDetector;

    private File videoFile;

    //for managing bitmap processing
    //private RenderScript renderScript;

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
        //this.renderScript = RenderScript.create(context); // where context can be your activity, application, etc.

		prefs = new PreferenceManager(context);

        motionDetector = new MotionDetector(
                motionSensitivity);

        motionDetector.addListener((detectedImage, rawBitmap, motionDetected) -> {

            for (MotionDetector.MotionListener listener : listeners)
                listener.onProcess(detectedImage,rawBitmap,motionDetected);

            if (motionDetected)
                mEncodeThreadPool.execute(() -> saveDetectedImage(rawBitmap));

        });
	/*
		 * We bind to the alert service
		 */
        this.context.bindService(new Intent(context,
                MonitorService.class), mConnection, Context.BIND_ABOVE_CLIENT);
	}

	private void saveDetectedImage (Bitmap rawBitmap)
    {
        if (serviceMessenger != null) {
            Message message = new Message();
            message.what = EventTrigger.CAMERA;

            try {

                File fileImageDir = new File(this.context.getExternalFilesDir(null), prefs.getDefaultMediaStoragePath());
                fileImageDir.mkdirs();

                String ts = new SimpleDateFormat(Utils.DATE_TIME_PATTERN,
                        Locale.getDefault()).format(new Date());

                File fileImage = new File(fileImageDir, ts.concat(".detected.original.jpg"));
                FileOutputStream stream = new FileOutputStream(fileImage);
                rawBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                stream.flush();
                stream.close();
                message.getData().putString(MonitorService.KEY_PATH, fileImage.getAbsolutePath());

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

	public void setMotionSensitivity (int
				motionSensitivity )
				{
				this.
				motionSensitivity = motionSensitivity;
                    motionDetector.setMotionSensitivity(motionSensitivity);
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
	public void startCamera() {


        updateCamera();

        cameraView.setPlaySounds(false);
        cameraView.setPreviewStreamSize(new SizeSelector() {
            @NonNull
            @Override
            public List<Size> select(@NonNull List<Size> source) {
                ArrayList<Size> result = new ArrayList<>();

                for (Size size : source)
                {
                    if (size.getWidth()<MAX_CAMERA_WIDTH)
                        result.add(size);
                }

                return result;
            }
        });
        cameraView.open();

        cameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                long now = System.currentTimeMillis();
                if (now < CameraViewHolder.this.lastTimestamp + DETECTION_INTERVAL_MS)
                    return;

                CameraViewHolder.this.lastTimestamp = now;
                byte[] data = frame.getData();
                Size size = frame.getSize();

                // Frame video encoding was here previously, but is now
                // done by android system functions in the CameraView library.

                mDecodeThreadPool.execute(() -> processNewFrame(data, size));
            }
        });


    }

    public void updateCamera ()
    {
        switch (prefs.getCamera()) {
            case PreferenceManager.FRONT:
                    cameraView.setFacing(Facing.FRONT);
                break;
            case PreferenceManager.BACK:
                    cameraView.setFacing(Facing.BACK);
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
    private final BlockingQueue<Runnable> mEncodeWorkQueue = new LinkedBlockingQueue<Runnable>();

    // Creates a thread pool manager
    private ThreadPoolExecutor mEncodeThreadPool = new ThreadPoolExecutor(
            1,       // Initial pool size
            1,       // Max pool size
            10,
            TimeUnit.SECONDS,
            mEncodeWorkQueue);


    private Matrix mtxVideoRotate;

    // recordNewFrame replaced by CameraView's internal android system encoding

    private void finishVideoEncoding ()
    {
        cameraView.stopVideo();

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
    }

    private void processNewFrame (byte[] data, Size size)
    {
        if (data != null && size != null) {
            int width = size.getWidth();
            int height = size.getHeight();

            motionDetector.detect(
                    lastPic,
                    data,
                    width,
                    height);

            lastPic = data;
        }
    }


	private synchronized boolean recordVideo() {

	    if (doingVideoProcessing)
	        return false;
        String ts1 = new SimpleDateFormat(Utils.DATE_TIME_PATTERN,
                Locale.getDefault()).format(new Date());
        File fileStoragePath = new File(Environment.getExternalStorageDirectory(),prefs.getDefaultMediaStoragePath());
        fileStoragePath.mkdirs();

        videoFile =  new File(fileStoragePath, ts1 + ".mp4");

	// jcodec encoding replaced by CameraView's android system encoding.

	// Once there is a place to send live streaming uploading, this could be
	// done with CameraView by adding FileDescriptor support to the interface,
	// which doesn't look hard to do, and then passing a FileDescriptor instead
	// of a File.  See https://github.com/natario1/CameraView/issues/967

        cameraView.takeVideoSnapshot(videoFile);

        mtxVideoRotate = new Matrix();

        if (cameraView.getFacing() == Facing.FRONT) {
            mtxVideoRotate.postRotate(-cameraView.getRotation());
            mtxVideoRotate.postScale(-1, 1, cameraView.getWidth() / 2, cameraView.getHeight() / 2);
        }
        else
           mtxVideoRotate.postRotate(cameraView.getRotation());

        doingVideoProcessing = true;

        int seconds = prefs.getMonitoringTime() * 1000;
        updateHandler.postDelayed(() -> {
           doingVideoProcessing = false;
            finishVideoEncoding();
        }, seconds);

        return true;
    }


    public synchronized void stopCamera ()
    {
        if (cameraView != null) {
           cameraView.close();
        }
    }


    public void destroy ()
    {
        if (mConnection != null) {
            this.context.unbindService(mConnection);
            mConnection = null;
        }
        stopCamera();
    }

    public int getCorrectCameraOrientation(Facing facing, int orientation) {

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
        if(facing == Facing.FRONT){
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
