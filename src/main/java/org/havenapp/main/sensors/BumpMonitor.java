package org.havenapp.main.sensors;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.service.MonitorService;

/**
 * Use the Significant Motion trigger sensor on API 18+
 *
 * Created by rockgecko on 27/12/17.
 */
@TargetApi(18)
public class BumpMonitor {

    // For shake motion detection.
    private SensorManager sensorMgr;

    /**
     * Accelerometer sensor
     */
    private Sensor bumpSensor;

    /**
     * Last update of the accelerometer
     */
    private long lastUpdate = -1;


    private final static int CHECK_INTERVAL = 1000;

    public BumpMonitor(Context context) {


        context.bindService(new Intent(context,
                MonitorService.class), mConnection, Context.BIND_ABOVE_CLIENT);

        sensorMgr = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);
        bumpSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        if (bumpSensor == null) {
            Log.i("BumpMonitor", "Warning: no significant motion sensor");
        } else {
            boolean registered = sensorMgr.requestTriggerSensor(sensorListener, bumpSensor);
            Log.i("BumpMonitor", "Significant motion sensor registered: "+registered);
        }

    }


    public void stop(Context context) {
        sensorMgr.cancelTriggerSensor(sensorListener, bumpSensor);
        context.unbindService(mConnection);
    }
    private TriggerEventListener sensorListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent event) {
            Log.i("BumpMonitor", "Sensor triggered");
            //value[0] = 1.0 when the sensor triggers. 1.0 is the only allowed value.
            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if (event.sensor.getType() == Sensor.TYPE_SIGNIFICANT_MOTION) {
                if ((curTime - lastUpdate) > CHECK_INTERVAL) {
                    lastUpdate = curTime;

                    /*
                     * Send Alert
                     */
                    Message message = new Message();
                    message.what = EventTrigger.BUMP;

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
            //re-register the listener (it finishes after each event)
            boolean registered = sensorMgr.requestTriggerSensor(sensorListener, bumpSensor);
            Log.i("BumpMonitor", "Significant motion sensor re-registered: "+registered);

        }
    };

    private Messenger serviceMessenger = null;

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i("BumpMonitor", "SERVICE CONNECTED");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            serviceMessenger = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName arg0) {
            Log.i("BumpMonitor", "SERVICE DISCONNECTED");
            serviceMessenger = null;
        }
    };

}
