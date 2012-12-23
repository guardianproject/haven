package me.ziccard.secureit.fragment;

import me.ziccard.secureit.R;
import me.ziccard.secureit.service.BluetoothService;
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
     * Shake threshold
     */
    private static final int SHAKE_THRESHOLD = 3000;
    
	private TextView accelerometerText;
	
	
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
								
				accelerometerText.setText("X: "+accel_values[0]+", Y: "+accel_values[1]+" Z: "+accel_values[2]);
				
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
						Message message = new Message();
						message.what = BluetoothService.ACCELEROMETER_MESSAGE;
						
						try {
							serviceMessenger.send(message);
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
		getActivity().bindService(new Intent(getActivity(), 
				BluetoothService.class), mConnection, Context.BIND_ABOVE_CLIENT);
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
