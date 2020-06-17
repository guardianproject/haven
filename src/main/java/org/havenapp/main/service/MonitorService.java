
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package org.havenapp.main.service;


import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.havenapp.main.HavenApp;
import org.havenapp.main.MonitorActivity;
import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.database.HavenEventDB;
import org.havenapp.main.model.Event;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.resources.ResourceManager;
import org.havenapp.main.sensors.AccelerometerMonitor;
import org.havenapp.main.sensors.AmbientLightMonitor;
import org.havenapp.main.sensors.BarometerMonitor;
import org.havenapp.main.sensors.BumpMonitor;
import org.havenapp.main.sensors.MicrophoneMonitor;
import org.havenapp.main.sensors.PowerConnectionReceiver;

import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

@SuppressLint("HandlerLeak")
public class MonitorService extends Service {

    /**
     * Monitor instance
     */
    private static MonitorService sInstance;

    /**
     * To show a notification on service start
     */
    private final static String channelId = "monitor_id";
    private final static CharSequence channelName = "Haven notifications";
    private final static String channelDescription= "Important messages from Haven";
	
    /**
     * Object used to retrieve shared preferences
     */
     private PreferenceManager mPrefs = null;

    /**
     * Sensor Monitors
     */
    private AccelerometerMonitor mAccelManager = null;
    private BumpMonitor mBumpMonitor = null;
    private MicrophoneMonitor mMicMonitor = null;
    private BarometerMonitor mBaroMonitor = null;
    private AmbientLightMonitor mLightMonitor = null;

    private PowerConnectionReceiver mPowerReceiver = null;

    private boolean mIsMonitoringActive = false;

    /**
     * Last Event instances
     */
    private Event mLastEvent;

    /**
     * Last sent notification time
     */
    private Date mLastNotification;

        /**
	 * Handler for incoming messages
	 */
    private class MessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {

		    //only accept alert if monitor is running
		    if (mIsMonitoringActive)
		        alert(msg.what,msg.getData().getString(KEY_PATH));
		}
	}

	public final static String KEY_PATH = "path";

    /**
     * An identifier to be used as {@link Message#what} while communicating with this
     * Service.
     * <p>
     * This denotes that the current running service must be stopped.
     */
    public static final int MSG_STOP_SELF = -2;
		
	/**
	 * Messenger interface used by clients to interact
	 */
	private final Messenger messenger = new Messenger(new MessageHandler());

    /**
     * Helps keep the service awake when screen is off
     */
    private PowerManager.WakeLock wakeLock;

    /**
     * Application
     */
    private HavenApp mApp = null;

	/**
	 * Called on service creation, sends a notification
	 */
    @Override
    public void onCreate() {

        sInstance = this;

        mApp = (HavenApp)getApplication();

        mPrefs = new PreferenceManager(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel();
        }

        startSensors();

        showNotification();

      //  startCamera();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
                "haven:MyWakelockTag");
        wakeLock.acquire();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupNotificationChannel ()
    {
        android.app.NotificationManager manager = (android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        android.app.NotificationChannel channel;
        channel = new android.app.NotificationChannel(channelId, channelName,
                android.app.NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(channelDescription);
        channel.setLightColor(Color.RED);
        channel.setImportance(android.app.NotificationManager.IMPORTANCE_MIN);
        manager.createNotificationChannel(channel);
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
				new NotificationCompat.Builder(this, channelId)
						.setSmallIcon(R.drawable.ic_stat_haven)
						.setContentTitle(getString(R.string.app_name))
						.setContentText(text);

		mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setWhen(System.currentTimeMillis());
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_SECRET);

		startForeground(1, mBuilder.build());

    }

    public boolean isRunning ()
    {
        return mIsMonitoringActive;

    }

    private void startSensors ()
    {
        mIsMonitoringActive = true;

        // set current event start date in prefs
        mPrefs.setCurrentSession(new Date(System.currentTimeMillis()));

        if (!mPrefs.getAccelerometerSensitivity().equals(PreferenceManager.OFF)) {
            mAccelManager = new AccelerometerMonitor(this);
            if(Build.VERSION.SDK_INT>=18) {
                mBumpMonitor = new BumpMonitor(this);
            }
        }

        //moving these out of the accelerometer pref, but need to enable off prefs for them too
        mBaroMonitor = new BarometerMonitor(this);
        mLightMonitor = new AmbientLightMonitor(this);

        mPrefs.activateMonitorService(true);

        if (mPrefs.getHeartbeatActive()){
            SignalSender sender = SignalSender.getInstance(this, mPrefs.getSignalUsername());
            sender.startHeartbeatTimer(mPrefs.getHeartbeatNotificationTimeMs());
        }

        // && !mPrefs.getVideoMonitoringActive()

        if (!mPrefs.getMicrophoneSensitivity().equals(PreferenceManager.OFF))
            mMicMonitor = new MicrophoneMonitor(this);

        mPowerReceiver = new PowerConnectionReceiver();
        // register our power status receivers
        IntentFilter powerConnectedFilter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        registerReceiver(mPowerReceiver, powerConnectedFilter);

        IntentFilter powerDisconnectedFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mPowerReceiver, powerDisconnectedFilter);
    }

    private void stopSensors ()
    {
        mIsMonitoringActive = false;
        //this will never be false:
        // -you can't use ==, != for string comparisons, use equals() instead
        // -Value is never set to OFF in the first place
        if (!mPrefs.getAccelerometerSensitivity().equals(PreferenceManager.OFF)) {
            mAccelManager.stop(this);
            if(Build.VERSION.SDK_INT>=18) {
                mBumpMonitor.stop(this);
            }
        }

        //moving these out of the accelerometer pref, but need to enable off prefs for them too
        mBaroMonitor.stop(this);
        mLightMonitor.stop(this);

        // && !mPrefs.getVideoMonitoringActive())

        if (!mPrefs.getMicrophoneSensitivity().equals(PreferenceManager.OFF))
            mMicMonitor.stop(this);

        if (mPrefs.getMonitorServiceActive()) {
            mPrefs.activateMonitorService(false);
            if (mPrefs.getHeartbeatActive()) {
                SignalSender sender = SignalSender.getInstance(this, mPrefs.getSignalUsername());
                sender.stopHeartbeatTimer();
            }
        }
        
        unregisterReceiver(mPowerReceiver);
    }

    /**
    * Sends an alert according to type of connectivity
    */
    public void alert(int alertType, String value) {

        Date now = new Date();
        boolean doNotification = false;

        //for the UI visual
        Intent iEvent = new Intent("event");
        iEvent.putExtra("type",alertType);
        LocalBroadcastManager.getInstance(this).sendBroadcast(iEvent);

        if (alertType == MSG_STOP_SELF) {
            stopForeground(true);
            stopSelf();
            return;
        }

        if (TextUtils.isEmpty(value))
            return;

        if (mLastEvent == null) {
            mLastEvent = new Event();
            long eventId = HavenEventDB.getDatabase(getApplicationContext())
                    .getEventDAO().insert(mLastEvent);
            mLastEvent.setId(eventId);
            doNotification = true;
        }
        else if (mPrefs.getNotificationTimeMs() == 0)
        {
            doNotification = true;
        }
        else if (mPrefs.getNotificationTimeMs() > 0 && mLastNotification != null)
        {
            //check if time window is within configured notification time window
            doNotification = ((now.getTime()-mLastNotification.getTime())>mPrefs.getNotificationTimeMs());
        }

        if (doNotification)
        {
            doNotification = !(mPrefs.getVideoMonitoringActive() && alertType == EventTrigger.CAMERA);
        }

        EventTrigger eventTrigger = new EventTrigger();
        eventTrigger.setType(alertType);
        eventTrigger.setPath(value);

        mLastEvent.addEventTrigger(eventTrigger);

        //we don't need to resave the event, only the trigger
        long eventTriggerId = HavenEventDB.getDatabase(getApplicationContext())
                .getEventTriggerDAO().insert(eventTrigger);
        eventTrigger.setId(eventTriggerId);

        if (doNotification) {

            mLastNotification = new Date();
            /*
             * If SMS mode is on we send an SMS or Signal alert to the specified
             * number
             */
            StringBuilder alertMessage = new StringBuilder();
            alertMessage.append(getString(R.string.intrusion_detected,
                    eventTrigger.getStringType(new ResourceManager(this))));

            if (mPrefs.isRemoteNotificationActive() && mPrefs.isSignalVerified()) {
                //since this is a secure channel, we can add the Onion address
                if (mPrefs.getRemoteAccessActive() && (!TextUtils.isEmpty(mPrefs.getRemoteAccessOnion()))) {
                    alertMessage.append(" http://").append(mPrefs.getRemoteAccessOnion())
                            .append(':').append(WebServer.LOCAL_PORT);
                }

                SignalSender sender = SignalSender.getInstance(this, mPrefs.getSignalUsername());
                ArrayList<String> recips = new ArrayList<>();
                StringTokenizer st = new StringTokenizer(mPrefs.getRemotePhoneNumber(), ",");
                while (st.hasMoreTokens())
                    recips.add(st.nextToken());

                String attachment = null;
                if (eventTrigger.getType() == EventTrigger.CAMERA) {
                    attachment = eventTrigger.getPath();
                } else if (eventTrigger.getType() == EventTrigger.MICROPHONE) {
                    attachment = eventTrigger.getPath();
                }
                else if (eventTrigger.getType() == EventTrigger.CAMERA_VIDEO) {
                    attachment = eventTrigger.getPath();
                }

                sender.sendMessage(recips, alertMessage.toString(), attachment, null);
            }
        }

    }




}
