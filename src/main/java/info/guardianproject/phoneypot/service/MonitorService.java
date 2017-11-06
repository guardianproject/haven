
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
import android.text.TextUtils;
import android.util.Log;

import org.asamk.Signal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import info.guardianproject.phoneypot.MonitorActivity;
import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.PreferenceManager;
import info.guardianproject.phoneypot.model.Event;
import info.guardianproject.phoneypot.model.EventTrigger;
import info.guardianproject.phoneypot.sensors.AccelerometerMonitor;
import info.guardianproject.phoneypot.sensors.AmbientLightMonitor;
import info.guardianproject.phoneypot.sensors.BarometerMonitor;
import info.guardianproject.phoneypot.sensors.MicrophoneMonitor;

@SuppressLint("HandlerLeak")
public class MonitorService extends Service {

    /**
     * Monitor instance
     */
    private static MonitorService sInstance;

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
    AmbientLightMonitor mLightMonitor = null;

    private boolean mIsRunning = false;
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

    /*
    ** Onion-available Web Server for optional remote access
     */
    WebServer mOnionServer = null;

	/**
	 * Called on service creation, sends a notification
	 */
    @Override
    public void onCreate() {

        sInstance = this;

        manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        prefs = new PreferenceManager(this);

        if (prefs.getRemoteAccessActive())
            startServer();

        startSensors();

        showNotification();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
    }

    public static MonitorService getInstance ()
    {
        return sInstance;
    }
    
    /**
     * Called on service destroy, cancels persistent notification
     * and shows a toast
     */
    @Override
    public void onDestroy() {

        wakeLock.release();
        stopSensors();

        if (mOnionServer != null)
            mOnionServer.stop();

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
						.setSmallIcon(R.drawable.ic_stat_haven)
						.setContentTitle(getString(R.string.app_name))
						.setContentText(text);

		mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setWhen(System.currentTimeMillis());

		startForeground(1, mBuilder.build());

    }

    public boolean isRunning ()
    {
        return mIsRunning;

    }

    private void startSensors ()
    {
        mIsRunning = true;

        if (prefs.getAccelerometerSensitivity() != PreferenceManager.OFF) {
            mAccelManager = new AccelerometerMonitor(this);
            mBaroMonitor = new BarometerMonitor(this);
            mLightMonitor = new AmbientLightMonitor(this);
        }

        if (prefs.getMicrophoneSensitivity() != PreferenceManager.OFF)
            mMicMonitor = new MicrophoneMonitor(this);


    }

    private void stopSensors ()
    {
        mIsRunning = false;

        if (prefs.getAccelerometerSensitivity() != PreferenceManager.OFF) {
            mAccelManager.stop(this);
            mBaroMonitor.stop(this);
            mLightMonitor.stop(this);
        }

        if (prefs.getMicrophoneSensitivity() != PreferenceManager.OFF)
            mMicMonitor.stop(this);
    }

    /**
    * Sends an alert according to type of connectivity
    */
    public synchronized void alert(int alertType, String path) {

        Date now = new Date();
        boolean isNewEvent = false;

        if (mLastEvent == null || (!mLastEvent.insideEventWindow(now)))
        {
            mLastEvent = new Event();
            mLastEvent.save();

            isNewEvent = true;
        }

        EventTrigger eventTrigger = new EventTrigger();
        eventTrigger.setType(alertType);
        eventTrigger.setPath(path);

        mLastEvent.addEventTrigger(eventTrigger);

        //we don't need to resave the event, only the trigger
        eventTrigger.save();

        /*
         * If SMS mode is on we send an SMS alert to the specified
         * number
         */
        if (isNewEvent) {
            //get the manager

            StringBuffer alertMessage = new StringBuffer();
            alertMessage.append(getString(R.string.intrusion_detected,eventTrigger.getStringType()));

            if (prefs.getSignalUsername() != null)
            {
                //since this is a secure channel, we can add the Onion address
                if (prefs.getRemoteAccessActive() && (!TextUtils.isEmpty(prefs.getRemoteAccessOnion())))
                {
                    alertMessage.append(" http://").append(prefs.getRemoteAccessOnion())
                            .append(':').append(WebServer.LOCAL_PORT);
                }

                SignalSender sender = SignalSender.getInstance(this,prefs.getSignalUsername());
                ArrayList<String> recips = new ArrayList<>();
                StringTokenizer st = new StringTokenizer(prefs.getSmsNumber(),",");
                while (st.hasMoreTokens())
                    recips.add(st.nextToken());

                String attachment = null;
                if (eventTrigger.getType() == EventTrigger.CAMERA)
                {
                    attachment = eventTrigger.getPath();
                }

                sender.sendMessage(recips,alertMessage.toString(), attachment);
            }
            else if (prefs.getSmsActivation())
            {
                SmsManager manager = SmsManager.getDefault();

                StringTokenizer st = new StringTokenizer(prefs.getSmsNumber(),",");
                while (st.hasMoreTokens())
                    manager.sendTextMessage(st.nextToken(), null, alertMessage.toString(), null, null);

            }



        }

    }

    private void startServer ()
    {
        try {
            mOnionServer = new WebServer();
            mOnionServer.setPassword("foobar");
        }
        catch (IOException ioe)
        {
            Log.e("OnioNServer","unable to start onion server",ioe);
        }
    }

}
