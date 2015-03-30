/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.service;

import me.ziccard.secureit.MonitorActivity;
import me.ziccard.secureit.R;
import me.ziccard.secureit.SecureItPreferences;
import me.ziccard.secureit.async.MicrophoneTaskFactory;
import me.ziccard.secureit.async.BluetoothServerTask;
import me.ziccard.secureit.async.MicrophoneTaskFactory.RecordLimitExceeded;
import me.ziccard.secureit.async.BluetoothServerTask.NoBluetoothException;
import me.ziccard.secureit.async.upload.BluetoothPeriodicPositionUploaderTask;
import me.ziccard.secureit.async.upload.ImagesUploaderTask;
import me.ziccard.secureit.async.upload.PeriodicPositionUploaderTask;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class UploadService extends Service {
	
	/**
	 * Task used to upload position, is periodic, when we close the app
	 * we need to stop the service and that task
	 */
	private AsyncTask<Void, Void, Void> positionTask = null;
	
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
	private SecureItPreferences prefs = null;
	
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
        prefs = new SecureItPreferences(this);
        
		try {
			new BluetoothServerTask(this).start();
		} catch (NoBluetoothException e) {
			Log.i("UploadService", "Background bluetooth server not started");
			CharSequence text = "Background bluetooth server not started";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
        
        showNotification();
    }
    
    /**
     * Called on service destroy, cancels persistent notification
     * and shows a toast
     */
    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        manager.cancel(R.string.secure_service_started);
        if (positionTask!=null && !positionTask.isCancelled()) {
        	positionTask.cancel(true);
        }

        // Tell the user we stopped.
        Toast.makeText(this, R.string.secure_service_stopped, Toast.LENGTH_SHORT).show();
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
    	
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.secure_service_started);
        
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
        		toLaunch, PendingIntent.FLAG_UPDATE_CURRENT);
        
        notification.setLatestEventInfo(this, "SecureService",
                text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        manager.notify(R.string.secure_service_started, notification);
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
    	* If remote communication with SecureIt back-end is required we
    	* need to check the type of connectivity
    	*/
		if (prefs.getRemoteActivation()) {
	    	ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
	    	boolean isConnected = false;
	    	boolean isWifi = false;
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			if (activeNetwork != null) {
				isConnected = activeNetwork.isConnectedOrConnecting();
				isWifi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
			}
			
			// Phone is connected
			if (isConnected) {
				
				int connectivityType = ImagesUploaderTask.NO_CONNECTIVITY;
				
				// through wireless
				if (isWifi) {
					CharSequence text = "WIFI: sending a lot of data";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(this, text, duration);
					toast.show();
					connectivityType = ImagesUploaderTask.WIFI_CONNECTIVITY;
					
				// through 3G
				} else {
					CharSequence text = "3G: sending a little of data";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(this, text, duration);
					toast.show();
					connectivityType = ImagesUploaderTask.MOBILE_CONNECTIVITY;
				}
				
				/*
				 * Image uploader task according to connectivity type
				 */
				(new ImagesUploaderTask(this, connectivityType)).execute();
				/*
				 * Audio recorder and uploader task
				 */
				try {
					MicrophoneTaskFactory.makeRecorder(this).start();
				} catch (RecordLimitExceeded e) {
					Log.e("UploadService", "An audio is being uploaded");
				}
				/*
				 * Periodic position uploader task 
				 */
				positionTask = new PeriodicPositionUploaderTask(this);
				positionTask.execute();	
			
			} else {
				CharSequence text = "NO CONNECTIVITY: sending a bluetooth alert";
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(this, text, duration);
				toast.show();
				
				positionTask = new BluetoothPeriodicPositionUploaderTask(this);
				positionTask.execute();	
			}
		}
		/*
		 * If SMS mode is on we send an SMS alert to the specified 
		 * number
		 */
		if (prefs.getSmsActivation()) {
			//get the manager
			SmsManager manager = SmsManager.getDefault();
			manager.sendTextMessage(prefs.getSmsNumber(), null, prefs.getSMSText(), null, null);
			
		}
    }   
}
