
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package info.guardianproject.phoneypot.service;


import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;

import java.util.Date;

import info.guardianproject.phoneypot.MonitorActivity;
import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.PreferenceManager;
import info.guardianproject.phoneypot.model.Event;
import info.guardianproject.phoneypot.model.EventTrigger;
import info.guardianproject.phoneypot.sensors.AccelerometerMonitor;
import info.guardianproject.phoneypot.sensors.BarometerMonitor;
import info.guardianproject.phoneypot.sensors.MicrophoneMonitor;

@SuppressLint("HandlerLeak")
public class MonitorService extends Service {

	/**
	 * To show a notification on service start
	 */
	private NotificationManager manager;

	/**
	* True only if service has been alerted by the accelerometer
	*/
	private boolean already_alerted;
	
	/**
	 * Object used to retrieve shared preferences
	 */
	private PreferenceManager prefs = null;


	/**
	 * Incrementing alert id
	 */
	int mNotificationAlertId = 7007;

    /**
     * Sensor Monitors
     */
    AccelerometerMonitor mAccelManager = null;
    MicrophoneMonitor mMicMonitor = null;
    BarometerMonitor mBaroMonitor = null;

    /**
     * Last Event instances
     */
    Event mLastEvent;

    /**
	 * Handler for incoming messages
	 */
	class MessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			alert(msg.what,msg.getData().getString("path"));
		}
	}
		
	/**
	 * Messenger interface used by clients to interact
	 */
	private final Messenger messenger = new Messenger(new MessageHandler());

    /*
    ** Helps keep the service awake when screen is off
     */
    PowerManager.WakeLock wakeLock;

	/**
	 * Called on service creation, sends a notification
	 */
    @Override
    public void onCreate() {
        manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        prefs = new PreferenceManager(this);

        startSensors();

        showNotification();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
    }
    
    /**
     * Called on service destroy, cancels persistent notification
     * and shows a toast
     */
    @Override
    public void onDestroy() {

        wakeLock.release();
        stopSensors();
		stopForeground(true);


    }
	
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
    
    /**
     * Show a notification while this service is running.
     */
    @SuppressWarnings("deprecation")
	private void showNotification() {

    	Intent toLaunch = new Intent(getApplicationContext(),
    	                                          MonitorActivity.class);

        toLaunch.setAction(Intent.ACTION_MAIN);
        toLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
        toLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        toLaunch,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.secure_service_started);

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_phone_alert)
						.setContentTitle(getString(R.string.app_name))
						.setContentText(text);

		mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setWhen(System.currentTimeMillis());

		startForeground(1, mBuilder.build());

    }

    private void startSensors ()
    {
        if (prefs.getAccelerometerSensitivity() != PreferenceManager.OFF) {
            mAccelManager = new AccelerometerMonitor(this);
            mBaroMonitor = new BarometerMonitor(this);
        }

        if (prefs.getMicrophoneSensitivity() != PreferenceManager.OFF)
            mMicMonitor = new MicrophoneMonitor(this);


    }

    private void stopSensors ()
    {
        if (prefs.getAccelerometerSensitivity() != PreferenceManager.OFF) {
            mAccelManager.stop(this);
            mBaroMonitor.stop(this);
        }

        if (prefs.getMicrophoneSensitivity() != PreferenceManager.OFF)
            mMicMonitor.stop(this);
    }

    /**
    * Sends an alert according to type of connectivity
    */
    private synchronized void alert(int alertType, String path) {

        Date now = new Date();
        boolean isNewEvent = false;

        if (mLastEvent == null)
        {
            mLastEvent = new Event();
            isNewEvent = true;
            mLastEvent.save();

        }
        else if (!mLastEvent.insideEventWindow(now))
        {
            //save the current event
            mLastEvent.save();

            //now create a new one
            mLastEvent = new Event();
            isNewEvent = true;
        }

        EventTrigger eventTrigger = new EventTrigger();
        eventTrigger.setType(alertType);
        eventTrigger.setPath(path);
        mLastEvent.addEventTrigger(eventTrigger);
        eventTrigger.save();

        /*
         * If SMS mode is on we send an SMS alert to the specified
         * number
         */
        if (isNewEvent && prefs.getSmsActivation()) {
            //get the manager

            StringBuffer alertMessage = new StringBuffer();
            alertMessage.append(getString(R.string.intrusion_detected));

            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(prefs.getSmsNumber(), null, alertMessage.toString(), null, null);

        }

    }


}
