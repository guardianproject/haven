/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package info.guardianproject.phoneypot.service;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.widget.Toast;

import info.guardianproject.phoneypot.MonitorActivity;
import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.PreferenceManager;

@SuppressLint("HandlerLeak")
public class MonitorService extends Service {

	/**
	 * To show a notification on service start
	 */
	private NotificationManager manager;
		
	/**
	 * Acceleration detected message
	 */
	public static final int ACCELEROMETER_MESSAGE = 0;
	
	/**
	 * Camera motion detected message
	 */
	public static final int CAMERA_MESSAGE = 1;
	
	/**
	 * Mic noise detected message
	 */
	public static final int MICROPHONE_MESSAGE = 2;

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
	 * Handler for incoming messages
	 */
	class MessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			alert(msg.what);
		}
	}
		
	/**
	 * Messenger interface used by clients to interact
	 */
	private final Messenger messenger = new Messenger(new MessageHandler());
	
	/**
	 * Called on service creation, sends a notification
	 */
    @Override
    public void onCreate() {
        manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        prefs = new PreferenceManager(this);
        

        showNotification();
    }
    
    /**
     * Called on service destroy, cancels persistent notification
     * and shows a toast
     */
    @Override
    public void onDestroy() {

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

   	   toLaunch.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
   		    |Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
   		    |Intent.FLAG_ACTIVITY_NEW_TASK);
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


        mBuilder.setContentIntent(resultPendingIntent);

		startForeground(0, mBuilder.build());


    }

    /**
    * Sends an alert according to type of connectivity
    */
    private void alert(int alertType) {

    	/*
    	 * If we have already received an alert 
    	 */
    	if (already_alerted) return;

    	/*
    	 * Alse we set an alert has bee received
    	 */
		already_alerted = true; 	
		

		/*
		 * If SMS mode is on we send an SMS alert to the specified 
		 * number
		 */
		if (prefs.getSmsActivation()) {
			//get the manager
			SmsManager manager = SmsManager.getDefault();
			manager.sendTextMessage(prefs.getSmsNumber(), null, prefs.getSMSText(), null, null);
			
		}

		showNotificationAlert("Got alert type: " + alertType);
    }

	private void showNotificationAlert (String message)
	{

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_phone_alert)
						.setContentTitle(getString(R.string.app_name))
						.setContentText(message);

		Intent resultIntent = new Intent(this, MonitorActivity.class);

// Because clicking the notification opens a new ("special") activity, there's
// no need to create an artificial back stack.
		PendingIntent resultPendingIntent =
				PendingIntent.getActivity(
						this,
						0,
						resultIntent,
						PendingIntent.FLAG_UPDATE_CURRENT
				);

		mBuilder.setContentIntent(resultPendingIntent);

		manager.notify(mNotificationAlertId, mBuilder.build());


	}
}
