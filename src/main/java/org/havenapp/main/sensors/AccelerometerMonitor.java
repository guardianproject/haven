package org.havenapp.main.sensors;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.FloatMath;
import android.util.Log;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.service.MonitorService;

/**
 * Created by n8fr8 on 3/10/17.
 */
public class AccelerometerMonitor implements SensorEventListener {

    // For shake motion detection.
    private SensorManager sensorMgr;

    /**
     * Accelerometer sensor
     */
    private Sensor accelerometer;

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
    private PreferenceManager prefs;

    /**
     * Shake threshold
     */
    private int shakeThreshold = -1;

    private float mAccelCurrent =  SensorManager.GRAVITY_EARTH;
    private float mAccelLast = SensorManager.GRAVITY_EARTH;
    private float mAccel = 0.00f;
    /**
     * Text showing accelerometer values
     */
    private int maxAlertPeriod = 30;
    private int remainingAlertPeriod = 0;
    private boolean alert = false;
    private final static int CHECK_INTERVAL = 100;

    public AccelerometerMonitor(Context context) {
        prefs = new PreferenceManager(context);

		/*
		 * Set sensitivity value
		 */
	try {
            shakeThreshold = Integer.parseInt(prefs.getAccelerometerSensitivity());
        }
        catch (Exception e)
        {
            shakeThreshold = 50;
        }

        context.bindService(new Intent(context,
                MonitorService.class), mConnection, Context.BIND_ABOVE_CLIENT);

        sensorMgr = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);
        accelerometer = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer == null) {
            Log.i("AccelerometerFrament", "Warning: no accelerometer");
        } else {
            sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Safe not to implement

    }

    public void onSensorChanged(SensorEvent event) {
        long curTime = System.currentTimeMillis();
        // only allow one update every 100ms.
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if ((curTime - lastUpdate) > CHECK_INTERVAL) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                accel_values = event.values.clone();

                if (alert && remainingAlertPeriod > 0) {
                    remainingAlertPeriod = remainingAlertPeriod - 1;
                } else {
                    alert = false;
                }

                if (last_accel_values != null) {

                    /**
                    float speed = Math.abs(
                            accel_values[0] + accel_values[1] + accel_values[2] -
                                    last_accel_values[0] + last_accel_values[1] + last_accel_values[2])
                            / diffTime * 1000;
                    **/
                    mAccelLast = mAccelCurrent;
                    mAccelCurrent =(float)Math.sqrt(accel_values[0]* accel_values[0] + accel_values[1]*accel_values[1]
                            + accel_values[2]*accel_values[2]);
                    float delta = mAccelCurrent - mAccelLast;
                    mAccel = mAccel * 0.9f + delta;

                    if (mAccel > shakeThreshold) {
						/*
						 * Send Alert
						 */

                        alert = true;
                        remainingAlertPeriod = maxAlertPeriod;

                        Message message = new Message();
                        message.what = EventTrigger.ACCELEROMETER;
                        message.getData().putString("path",mAccel+"");

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

    public void stop(Context context) {
        sensorMgr.unregisterListener(this);
        context.unbindService(mConnection);
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
