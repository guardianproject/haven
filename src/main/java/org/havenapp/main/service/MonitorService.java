
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package org.havenapp.main.service;


import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import org.havenapp.main.HavenApp;
import org.havenapp.main.MonitorActivity;
import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.model.Event;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.sensors.AccelerometerMonitor;
import org.havenapp.main.sensors.AmbientLightMonitor;
import org.havenapp.main.sensors.BarometerMonitor;
import org.havenapp.main.sensors.BumpMonitor;
import org.havenapp.main.sensors.MicrophoneMonitor;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.StringTokenizer;

import javax.mail.MessagingException;

@SuppressLint("HandlerLeak")
public class MonitorService extends Service {

    final static String channelId = "monitor_id";
    final static CharSequence channelName = "Haven notifications";
    final static String channelDescription = "Important messages from Haven";
    /**
     * Monitor instance
     */
    private static MonitorService sInstance;
    /**
     * Messenger interface used by clients to interact
     */
    private final Messenger messenger = new Messenger(new MessageHandler());
    /**
     * To show a notification on service start
     */
    NotificationManager manager;
    NotificationChannel mChannel;
    /**
     * Incrementing alert id
     */
    int mNotificationAlertId = 7007;
    /**
     * Sensor Monitors
     */
    AccelerometerMonitor mAccelManager = null;
    BumpMonitor mBumpMonitor = null;
    MicrophoneMonitor mMicMonitor = null;
    BarometerMonitor mBaroMonitor = null;
    AmbientLightMonitor mLightMonitor = null;
    /**
     * Last Event instances
     */
    Event mLastEvent;
    /*
    ** Helps keep the service awake when screen is off
     */
    PowerManager.WakeLock wakeLock;
    /*
    **
    * Application
     */
    HavenApp mApp = null;
    /**
     * True only if service has been alerted by the accelerometer
     */
    private boolean already_alerted;
    /**
     * Object used to retrieve shared preferences
     */
    private PreferenceManager mPrefs = null;
    private boolean mIsRunning = false;

    public static MonitorService getInstance() {
        return sInstance;
    }

    /**
     * Called on service creation, sends a notification
     */
    @Override
    public void onCreate() {

        sInstance = this;

        mApp = (HavenApp) getApplication();

        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mPrefs = new PreferenceManager(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription(channelDescription);
            mChannel.setLightColor(Color.RED);
            mChannel.setImportance(NotificationManager.IMPORTANCE_MIN);
            manager.createNotificationChannel(mChannel);
        }

        startSensors();

        showNotification();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        assert powerManager != null;
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
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

    public boolean isRunning() {
        return mIsRunning;

    }

    private void startSensors() {
        mIsRunning = true;

        if (mPrefs.getAccelerometerSensitivity() != PreferenceManager.OFF) {
            mAccelManager = new AccelerometerMonitor(this);
            if (Build.VERSION.SDK_INT >= 18) {
                mBumpMonitor = new BumpMonitor(this);
            }
        }

        //moving these out of the accelerometer pref, but need to enable off prefs for them too
        mBaroMonitor = new BarometerMonitor(this);
        mLightMonitor = new AmbientLightMonitor(this);

        if (mPrefs.getMicrophoneSensitivity() != PreferenceManager.OFF)
            mMicMonitor = new MicrophoneMonitor(this);


    }

    private void stopSensors() {
        mIsRunning = false;
        //this will never be false:
        // -you can't use ==, != for string comparisons, use equals() instead
        // -Value is never set to OFF in the first place
        if (mPrefs.getAccelerometerSensitivity() != PreferenceManager.OFF) {
            mAccelManager.stop(this);
            if (Build.VERSION.SDK_INT >= 18) {
                mBumpMonitor.stop(this);
            }
        }

        //moving these out of the accelerometer pref, but need to enable off prefs for them too
        mBaroMonitor.stop(this);
        mLightMonitor.stop(this);

        if (mPrefs.getMicrophoneSensitivity() != PreferenceManager.OFF)
            mMicMonitor.stop(this);
    }

    /**
     * Sends an alert according to type of connectivity
     */
    public synchronized void alert(int alertType, String path) {

        Date now = new Date();
        boolean isNewEvent = false;

        if (mLastEvent == null || (!mLastEvent.insideEventWindow(now))) {
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
         * If SMS mode is on we send an SMS or Signal alert to the specified
         * number
         */
        StringBuilder alertMessage = new StringBuilder();
        alertMessage.append(getString(R.string.intrusion_detected, eventTrigger.getStringType(this)));

        // removing toast, but we should have some visual feedback for testing on the monitor screen
        //        Toast.makeText(this,alertMessage.toString(),Toast.LENGTH_SHORT).show();

        if (mPrefs.getIsEmailAlertsActive() && !mPrefs.getMailAddress().isEmpty() && !mPrefs.getMaillPassword().isEmpty()) {
            String attachment;
            if (eventTrigger.getType() == EventTrigger.CAMERA) {
                attachment = eventTrigger.getPath();
                new SendMailTask(attachment, eventTrigger.getTriggerTime()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else if (eventTrigger.getType() == EventTrigger.MICROPHONE) {
                attachment = eventTrigger.getPath();
                new SendMailTask(attachment, eventTrigger.getTriggerTime()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
        if (mPrefs.getSignalUsername() != null) {
            //since this is a secure channel, we can add the Onion address
            if (mPrefs.getRemoteAccessActive() && (!TextUtils.isEmpty(mPrefs.getRemoteAccessOnion()))) {
                alertMessage.append(" http://").append(mPrefs.getRemoteAccessOnion())
                        .append(':').append(WebServer.LOCAL_PORT);
            }

            SignalSender sender = SignalSender.getInstance(this, mPrefs.getSignalUsername());
            ArrayList<String> recips = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(mPrefs.getSmsNumber(), ",");
            while (st.hasMoreTokens())
                recips.add(st.nextToken());

            String attachment = null;
            if (eventTrigger.getType() == EventTrigger.CAMERA) {
                attachment = eventTrigger.getPath();
            } else if (eventTrigger.getType() == EventTrigger.MICROPHONE) {
                attachment = eventTrigger.getPath();
            }

            sender.sendMessage(recips, alertMessage.toString(), attachment);
        } else if (mPrefs.getSmsActivation() && isNewEvent) {
            SmsManager manager = SmsManager.getDefault();

            StringTokenizer st = new StringTokenizer(mPrefs.getSmsNumber(), ",");
            while (st.hasMoreTokens())
                manager.sendTextMessage(st.nextToken(), null, alertMessage.toString(), null, null);

        }
    }

    /**
     * Handler for incoming messages
     */
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            alert(msg.what, msg.getData().getString("path"));
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class SendMailTask extends AsyncTask<String, String, String> {
        String attachment;
        Date date;

        SendMailTask(String attachment, Date triggerTime) {
            this.attachment = attachment;
            this.date = triggerTime;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                Log.i("SendMailTask", "About to instantiate GMail...");
                GMail androidEmail = new GMail(mPrefs.getMailAddress(),
                        mPrefs.getMaillPassword(), new ArrayList<>(Collections.singletonList(mPrefs.getMailAddress())), "Haven Alert",
                        "Intrusion Detected", attachment, date);
                try {
                    androidEmail.createEmailMessage();
                    androidEmail.sendEmail();
                } catch (MessagingException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i("SendMailTask", "Mail Sent.");
            } catch (Exception e) {
                Log.e("SendMailTask", e.getMessage(), e);
            }
            return null;
        }

        protected void onPreExecute() {

        }

        @Override
        public void onPostExecute(String value) {
        }

    }

}
