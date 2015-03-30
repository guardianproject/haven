/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.fragment;

import me.ziccard.secureit.R;
import me.ziccard.secureit.SecureItPreferences;
import me.ziccard.secureit.service.UploadService;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import me.ziccard.secureit.opengl.AccelerometerGLSurfaceView;

public class AccelerometerFragment extends Fragment implements SensorEventListener {
		
    // For shake motion detection.
    private SensorManager sensorMgr;
    
    /**
     * Accelerometer sensor
     */
    private Sensor accelerometer;
    
    /**
     * OpenGL view to show accelerometer informations
     */
    private AccelerometerGLSurfaceView view;
    
    /**
     * Last update of the accelerometer
     */
    private long lastUpdate = -1;
    
    /**
     * Current accelerometer values
     */
    private float accel_values[];
    
    /**
     * Last accelerometer values
     */
    private float last_accel_values[];
    
    /**
     * Data field used to retrieve application prefences
     */
    private SecureItPreferences prefs;
    
    /**
     * Shake threshold
     */
    private static int SHAKE_THRESHOLD = 2300;
    
    /**
     * Text showing accelerometer values
     */
	private TextView accelerometerText;
	private int maxAlertPeriod = 30;
	private int remainingAlertPeriod = 0;
	private boolean alert = false;
	
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Safe not to implement
		
	}

	public void onSensorChanged(SensorEvent event) {
	    long curTime = System.currentTimeMillis();
	    // only allow one update every 100ms.
	    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		    if ((curTime - lastUpdate) > 100) {
				long diffTime = (curTime - lastUpdate);
				lastUpdate = curTime;
		 
				accel_values = event.values.clone();
				
				if (alert && remainingAlertPeriod > 0) {
				  remainingAlertPeriod = remainingAlertPeriod - 1;
				} else {
				  accelerometerText.setVisibility(View.INVISIBLE);
				  alert = false;
				}
				
				view.renderer.setPosition(-accel_values[0], accel_values[1], accel_values[2]);
		    					 
				if (last_accel_values != null) {
					float speed = Math.abs(
							accel_values[0]+accel_values[1]+accel_values[2] - 
							last_accel_values[0]+last_accel_values[1]+last_accel_values[2])
			                / diffTime * 10000;
					if (speed > SHAKE_THRESHOLD) {		
						/*
						 * Send Alert
						 */
					  
					    alert = true;
					    remainingAlertPeriod = maxAlertPeriod;
					    accelerometerText.setVisibility(View.VISIBLE);
					  
						Message message = new Message();
						message.what = UploadService.ACCELEROMETER_MESSAGE;
						
						try {
						  if (serviceMessenger != null) {
							serviceMessenger.send(message);
						  }
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	
					}
				}
				last_accel_values = accel_values.clone();
		    }
	    }  
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.accelerometer_fragment, container, false);

	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = new SecureItPreferences(getActivity());
		
		/*
		 * Set sensitivity value
		 */
		if (prefs.getAccelerometerSensitivity().equals("Medium")) {
			SHAKE_THRESHOLD = 2700;
			Log.i("AccelerometerFragment", "Sensitivity set to 2700");
		} else if (prefs.getAccelerometerSensitivity().equals("Low")) {
			SHAKE_THRESHOLD = 3100;
			Log.i("AccelerometerFragment", "Sensitivity set to 3100");
		} else {
			SHAKE_THRESHOLD = 2300;
			Log.i("AccelerometerFragment", "Sensitivity set to 2300");
		}
		
		getActivity().bindService(new Intent(getActivity(), 
				UploadService.class), mConnection, Context.BIND_ABOVE_CLIENT);
    }
 
    @Override
    public void onPause() {
    	super.onPause();
    	//sensorMgr.unregisterListener(this);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	sensorMgr.unregisterListener(this);
    	getActivity().unbindService(mConnection);
    	Log.i("AccelerometerFragment", "Fragment destroyed");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	view = new AccelerometerGLSurfaceView(this.getActivity());    	
    	((FrameLayout) getActivity().findViewById(R.id.opengl)).addView(view);
    	
		accelerometerText = (TextView) getActivity().findViewById(R.id.accelerometer_text);
		
		sensorMgr = (SensorManager) getActivity().getSystemService(Activity.SENSOR_SERVICE);
		accelerometer = (Sensor) sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		if (accelerometer == null) {
			Log.i("AccelerometerFrament", "Warning: no accelerometer");  	
		} else {
			sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		}
    }
    
	private Messenger serviceMessenger = null;
	
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	Log.i("AccelerometerFragment", "SERVICE CONNECTED");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            serviceMessenger = new Messenger(service);
        }
        
        public void onServiceDisconnected(ComponentName arg0) {
        	Log.i("AccelerometerFragment", "SERVICE DISCONNECTED");
            serviceMessenger = null;
        }
    };

}
