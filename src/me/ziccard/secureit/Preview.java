package me.ziccard.secureit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.ziccard.secureit.async.MotionAsyncTask;
import me.ziccard.secureit.async.MotionAsyncTask.MotionListener;
import me.ziccard.secureit.codec.ImageCodec;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;
import android.view.WindowManager;
import android.view.Display;
import android.widget.Toast;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {
	
	private List<MotionListener> listeners = new ArrayList<MotionListener>();
	
	/**
	 * Timestamp of the last picture processed
	 */
	private long lastTimestamp;
	/**
	 * Last picture processed
	 */
	private int[] lastPic;
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
	 * Max number of pictures stored
	 */
	private static final int MAX_IMAGES = 10;
	
	/**
	 * PATH to the directory where to save images
	 */
	private static final String FILE = "/secureit";
	
	
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
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		camera = Camera.open();
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
					        
					        imageCount = (imageCount + 1)%11;
					        
					        File file = new File(Environment.getExternalStorageDirectory() 
					                .getPath() + FILE + imageCount + ".jpg");
					        
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
								updateHandler);
						for (MotionListener listener : listeners) {
							Log.i("Preview", "Added listener");
							task.addListener(listener);
						}
						doingProcessing = true;
						task.addListener(new MotionListener() {
							
							public void onProcess(Bitmap oldBitmap, Bitmap newBitmap,
									boolean motionDetected) {
								Log.i("MotionListener", "Allowing further processing");
								doingProcessing = false;								
							}
						});
						task.start();
						lastPic = ImageCodec.N21toLuma(
								data, 
								size.width,
								size.height);
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
		
		camera.stopPreview();
		camera.release();
        camera = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(w, h);

        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        if(display.getRotation() == Surface.ROTATION_0)
        {
            parameters.setPreviewSize(h, w);                           
            camera.setDisplayOrientation(90);
        }

        if(display.getRotation() == Surface.ROTATION_90)
        {
            parameters.setPreviewSize(w, h);   
        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            parameters.setPreviewSize(h, w);
        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            parameters.setPreviewSize(w, h);
            camera.setDisplayOrientation(180);
        }
		camera.setParameters(parameters);
		camera.startPreview();
	}	
}