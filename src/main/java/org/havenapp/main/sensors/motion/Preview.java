
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
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.sensors.media.ImageCodec;
import org.havenapp.main.sensors.media.MediaRecorderTask;
import org.havenapp.main.sensors.media.MotionAsyncTask;
import org.havenapp.main.service.MonitorService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {

    /**
     * Object to retrieve and set shared preferences
     */
    private PreferenceManager prefs;
    private int cameraFacing = 0;

    private final static int PREVIEW_INTERVAL = 500;

    private List<MotionAsyncTask.MotionListener> listeners = new ArrayList<>();

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
	private Messenger serviceMessenger = null;
	private MediaRecorder mediaRecorder = null;
    private SurfaceHolder mHolder;
	private Camera camera;
	private Context context;
	private MotionAsyncTask task;
    private String videoFile;
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

	public Preview (Context context) {
		super(context);
		this.context = context;
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		prefs = new PreferenceManager(context);
		
		motionSensitivity = prefs.getCameraSensitivity();
	/*
		 * We bind to the alert service
		 */
        context.bindService(new Intent(context,
                MonitorService.class), mConnection, Context.BIND_ABOVE_CLIENT);}

				public void setMotionSensitivity (int
				motionSensitivity )
				{
				this.
				motionSensitivity = motionSensitivity;
	}
	
	public void addListener(MotionAsyncTask.MotionListener listener) {
		listeners.add(listener);
	}
	

	/**
	 * Called on the creation of the surface:
	 * setting camera parameters to lower possible resolution
	 * (preferred is 640x480)
	 * in order to minimize CPU usage
	 */
	public void surfaceCreated(SurfaceHolder holder) {


	    if (camera != null)
	        stopCamera();
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
							camera = Camera.open(camIdx);
							cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
						} catch (RuntimeException e) {
							Log.e("Preview", "Camera failed to open: " + e.getLocalizedMessage());
						}
					}
				}
				break;
			case PreferenceManager.BACK:

				camera = Camera.open();
				cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
				break;
			default:
				camera = null;
				break;
		}

        if (camera != null) {

            final Camera.Parameters parameters = camera.getParameters();

            try {
                List<Size> sizesPreviews = parameters.getSupportedPreviewSizes();

                Size bestSize = sizesPreviews.get(0);

                for (int i = 1; i < sizesPreviews.size(); i++) {
                    if ((sizesPreviews.get(i).width * sizesPreviews.get(i).height) >
                            (bestSize.width * bestSize.height)) {
                        bestSize = sizesPreviews.get(i);
                    }
                }

                parameters.setPreviewSize(bestSize.width, bestSize.height);

            } catch (Exception e) {
                Log.w("Camera", "Error setting camera preview size", e);
            }

            try {
                List<int[]> ranges = parameters.getSupportedPreviewFpsRange();
                int[] bestRange = ranges.get(0);
                for (int i = 1; i < ranges.size(); i++) {
                    if (ranges.get(i)[1] >
                            bestRange[1]) {
                        bestRange[0] = ranges.get(i)[0];
                        bestRange[1] = ranges.get(i)[1];

                    }
                }
                parameters.setPreviewFpsRange(bestRange[0], bestRange[1]);
            } catch (Exception e) {
                Log.w("Camera", "Error setting frames per second", e);
            }

            try {
                parameters.setAutoExposureLock(false);
                parameters.setExposureCompensation(parameters.getMaxExposureCompensation());
            } catch (Exception e) {
            }
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
                        if (now < Preview.this.lastTimestamp + PREVIEW_INTERVAL)
                            return;
                        if (!doingProcessing) {


							Log.i("Preview", "Processing new image");
							Preview.this.lastTimestamp = now;
							 task = new MotionAsyncTask(
									lastPic,
									data,
									size.width,
									size.height,
									updateHandler,
									motionSensitivity);
							for (MotionAsyncTask.MotionListener listener : listeners) {
								Log.i("Preview", "Added listener");
								task.addListener(listener);
							}
							doingProcessing = true;
							task.addListener(new MotionAsyncTask.MotionListener() {

                                public void onProcess(Bitmap oldBitmap, Bitmap newBitmap,
                                                      Bitmap rawBitmap,
                                                      boolean motionDetected) {

                                    if (motionDetected) {
                                        Log.i("MotionListener", "Motion detected");
                                        if (serviceMessenger != null) {
                                            Message message = new Message();
                                            message.what = EventTrigger.CAMERA;


                                            try {

                                                File fileImageDir = new File(Environment.getExternalStorageDirectory(), prefs.getImagePath());
                                                fileImageDir.mkdirs();

                                                String ts = new Date().getTime() + ".jpg";

												File fileImage = new File(fileImageDir, "detected.original." + ts);
												FileOutputStream stream = new FileOutputStream(fileImage);
												if (prefs.getCamera().equalsIgnoreCase(PreferenceManager.BACK)) {
													Bitmap bmps = ImageCodec.rotate(rawBitmap, 180, false);
													bmps.compress(Bitmap.CompressFormat.JPEG, 100, stream);} else {
													rawBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
												}
												stream.flush();
												stream.close();
												message.getData().putString("path", fileImage.getAbsolutePath());
                                                if (!doingVideoProcessing && prefs.getVideoMonitoringActive()) {
                                                    record(camera, serviceMessenger);
                                                }
												/**
												fileImage = new File(fileImageDir, "detected.match." + ts);
												stream = new FileOutputStream(fileImage);
												oldBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
												stream.flush();
												stream.close();

                                                 message.getData().putString("path", fileImage.getAbsolutePath());
                                                 **/
                                                serviceMessenger.send(message);

                                            } catch (Exception e) {
                                                // Cannot happen
                                                Log.e("Preview", "error creating imnage", e);
                                            }
                                        }
                                    }
                                    Log.i("MotionListener", "Allowing further processing");
                                    doingProcessing = false;
                                }
                            });
                            task.start();
                            lastPic = data;
                            try {

                                Camera.Parameters parameters = cam.getParameters();
                                parameters.setExposureCompensation(parameters.getMaxExposureCompensation());
                                cam.setParameters(parameters);

                            } catch (Exception e) {
                            }
                        }
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    Handler handler = new Handler();
    void record(Camera cam, Messenger messenger) {
        String ts1 = String.valueOf(new Date().getTime());
        videoFile = Environment.getExternalStorageDirectory() + File.separator + prefs.getImagePath() + File.separator + ts1 + ".mp4";
        int seconds = prefs.getMonitoringTime() * 1000;
        MediaRecorderTask mediaRecorderTask = new MediaRecorderTask(cam, videoFile, seconds);
        mediaRecorder = mediaRecorderTask.getPreparedMediaRecorder();
        AudioManager audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC,true);
        }
        mediaRecorder.start();
        doingVideoProcessing = true;
        handler.postDelayed(() -> {
            if (messenger != null) {
                Message message = new Message();
                message.what = EventTrigger.CAMERA_VIDEO;
                message.getData().putString("path", videoFile);
                try {
                    messenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                if (audioManager != null) {
                    audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                doingVideoProcessing = false;
            }
        }, seconds);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

        if (doingVideoProcessing && serviceMessenger != null && prefs.getVideoMonitoringActive()) {
            Message message = new Message();
            message.what = EventTrigger.CAMERA_VIDEO;
            message.getData().putString("path", videoFile);
            try {
                serviceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopCamera ()
    {
        if (camera != null) {
            // Surface will be destroyed when we return, so stop the preview.
            // Because the CameraDevice object is not a shared resource, it's very
            // important to release it when the activity is paused.
            if (serviceMessenger != null)
                context.unbindService(mConnection);

            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (camera != null) {

            int degree = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            int displayOrientation = 0;

            if (prefs.getCamera().equals(PreferenceManager.FRONT)) {

                switch (degree) {
                    case Surface.ROTATION_0:
                        displayOrientation = 90;
                        break;
                    case Surface.ROTATION_90:
                        displayOrientation = 0;
                        break;
                    case Surface.ROTATION_180:
                        displayOrientation = 0;
                        break;
                    case Surface.ROTATION_270:
                        displayOrientation = 180;
                        break;
                }
            } else {
                boolean isLandscape = false;// degree == Configuration.ORIENTATION_LANDSCAPE;

                switch (degree) {
                    case Surface.ROTATION_0:
                        displayOrientation = isLandscape ? 0 : 90;
                        break;
                    case Surface.ROTATION_90:
                        displayOrientation = isLandscape ? 0 : 270;
                        break;
                    case Surface.ROTATION_180:
                        displayOrientation = isLandscape ? 180 : 270;
                        break;
                    case Surface.ROTATION_270:
                        displayOrientation = isLandscape ? 180 : 90;
                        break;
                }
            }

            camera.setDisplayOrientation(displayOrientation);

            camera.startPreview();
        }
    }

    public int getCameraFacing() {
        return this.cameraFacing;
    }
}
