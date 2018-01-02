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
import android.util.Log;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.service.MonitorService;

/**
 * Created by n8fr8 on 3/10/17.
 */
public class AmbientLightMonitor implements SensorEventListener {

    // For shake motion detection.
    private SensorManager sensorMgr;

    /**
     * Accelerometer sensor
     */
    private Sensor sensor;

    /**
     * Last update of the accelerometer
     */
    private long lastUpdate = -1;

    /**
     * Current accelerometer values
     */
    private float current_values[];

    /**
     * Last accelerometer values
     */
    private float last_values[];

    /**
     * Data field used to retrieve application prefences
     */
    private PreferenceManager prefs;

    private final static float LIGHT_CHANGE_THRESHOLD = 100f;

    private int maxAlertPeriod = 30;
    private int remainingAlertPeriod = 0;
    private boolean alert = false;
    private final static int CHECK_INTERVAL = 1000;

    public AmbientLightMonitor(Context context) {
        prefs = new PreferenceManager(context);

        context.bindService(new Intent(context,
                MonitorService.class), mConnection, Context.BIND_ABOVE_CLIENT);

        sensorMgr = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);
        //noinspection RedundantCast
        sensor = (Sensor) sensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (sensor == null) {
            Log.i("AccelerometerFrament", "Warning: no accelerometer");
        } else {
            sensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Safe not to implement

    }

    public void onSensorChanged(SensorEvent event) {
        long curTime = System.currentTimeMillis();
        // only allow one update every 100ms.
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            if ((curTime - lastUpdate) > CHECK_INTERVAL) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                current_values = event.values.clone();

                if (alert && remainingAlertPeriod > 0) {
                    remainingAlertPeriod = remainingAlertPeriod - 1;
                } else {
                    alert = false;
                }

                if (last_values != null) {

                    boolean isChanged = false;

                    float lightChangedValue = Math.abs(last_values[0]-current_values[0]);

                    Log.d("LightSensor","Light changed: " + lightChangedValue);

                    //see if light value changed more than 10 values
                    isChanged = lightChangedValue >LIGHT_CHANGE_THRESHOLD;


                    if (isChanged) {
						/*
						 * Send Alert
						 */

                        alert = true;
                        remainingAlertPeriod = maxAlertPeriod;

                        Message message = new Message();
                        message.what = EventTrigger.LIGHT;
                        message.getData().putString("path",lightChangedValue+"");

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
                last_values = current_values.clone();
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
