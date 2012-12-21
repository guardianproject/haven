package me.ziccard.secureit.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

import me.ziccard.secureit.MonitorActivity;
import me.ziccard.secureit.R;
import me.ziccard.secureit.async.BluetoothServerTask;
import me.ziccard.secureit.async.BluetoothServerTask.NoBluetoothException;
import me.ziccard.secureit.bluetooth.ObjectBluetoothSocket;
import me.ziccard.secureit.config.Remote;
import me.ziccard.secureit.messages.BluetoothMessage;
import me.ziccard.secureit.messages.HelloMessage;
import me.ziccard.secureit.messages.KeyRequest;
import me.ziccard.secureit.messages.KeyResponse;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class BluetoothService extends Service {
	
	/**
	 * To show a notification on service start
	 */
	private NotificationManager manager;
	
	/**
	 * Registered clients to the service
	 */
	//private ArrayList<Messenger> clients = new ArrayList<Messenger>();
	
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
	private boolean accelerometer_alerted;

	/**
	* True only if service has been alerted by the camera
	**/
	private boolean camera_alerted;

	/**
	* True only if service has been alerted by the camera
	**/
	private boolean microphone_alerted;


	
	/**
	 * Adapter for bluetooth services
	 */
	private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	
	/**
	 * Handler for incoming messages
	 */
	class MessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			alert(msg.what);
		}
	}
	
	// Create a BroadcastReceiver for ACTION_FOUND and ACTION_DISCOVERY_FINISHED
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		
		private ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

		@Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            devices.add(device);
	            

	            Log.i("BluetoothService", "DISCOVERED "+device.getName());
	    		CharSequence text = "Discovered "+device.getName();
	    		int duration = Toast.LENGTH_SHORT;
	    		Toast toast = Toast.makeText(BluetoothService.this, text, duration);
	    		toast.show();

	            return;
	        }
	        // When ending the discovery
	        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
	        	for (BluetoothDevice device : devices){
		        	Log.i("DISCOVERY", "FINISHED");
		            try {
			            BluetoothSocket tmp = null;
						tmp = device.createInsecureRfcommSocketToServiceRecord(Remote.BLUETOOTH_UUID);
		
		            	if (tmp != null) {
		            		
		            		Log.i("BluetoothService", "Trying to connect to " + device.getName());
		            		
		            		adapter.cancelDiscovery();
		            		tmp.connect();
		            		
		            		Log.i("BluetoothService", "Connected to " + device.getName());
		            		
		            		ObjectBluetoothSocket socket = new ObjectBluetoothSocket(tmp);    		           		
		            		
		            		//Sending hello message
		            		BluetoothMessage message = new HelloMessage(); 			    	
		   			    	ObjectOutputStream ostream = socket.getOutputStream();
		   			    	message = new HelloMessage();
		   			    	ostream.writeObject(message);
		   			    	Log.i("BluetoothServerTask", "Sent message " + message.getType().toString());
		   			    	
		   			    	//Receiving key request
		   			    	ObjectInputStream instream = socket.getInputStream();
		   			    	message = (BluetoothMessage) instream.readObject();
		   			    	Log.i("BluetoothServerTask", "Received message "+message.getType().toString()); 
		   			    	
		   			    	message = new KeyResponse();
		   			    	ostream.writeObject(message);
		   			    	Log.i("BluetoothServerTask", "Sent message " + message.getType().toString());
		   			    	
		   			    	socket.close();
		            			            		
		            		CharSequence text = "Written message";
		        			int duration = Toast.LENGTH_SHORT;
		        			Toast toast = Toast.makeText(BluetoothService.this, text, duration);
		        			toast.show();  		
		            	}
					} catch (Exception e) {
						e.printStackTrace();
					}
	           }	        	
	        	unregisterReceiver(broadcastReceiver);
	        }
	    }
	};
	
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
        
		try {
			new BluetoothServerTask().start();
		} catch (NoBluetoothException e) {
			CharSequence text = "Sorry, bluetooth is off!";
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
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.secure_service_started);
        
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MonitorActivity.class), 0);
        
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

    	if (accelerometer_alerted && camera_alerted && microphone_alerted) return;

		accelerometer_alerted = true;
		camera_alerted = true;
		microphone_alerted = true;    	

    	/*
    	* Need to check the type o connectivity
    	*/
    	ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork.isConnectedOrConnecting();
		boolean isWifi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
		// Phone is connected
		if (isConnected) {
			// through wireless
			if (isWifi) {
				CharSequence text = "WIFI: we are sending a lot of data";
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(this, text, duration);
				toast.show();
				//(new DataUploaderTask(null, null, DataUploaderTask.WIFI_CONNECTIVITY)).execute();
			// through 3G
			} else {
				CharSequence text = "3G: we are sending a little of data";
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(this, text, duration);
				toast.show();
				//(new DataUploaderTask(null, null, DataUploaderTask.3G_CONNECTIVITY)).execute();
			}
		} else {
			CharSequence text = "NO CONNECTIVITY: we are sending an alert";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
			//sendBluetoothAlert();
		}
		sendBluetoothAlert();
    }
    
    /**
     * Discovers clients and sends help alerts
     */
    private void sendBluetoothAlert() {

    	registerReceiver(broadcastReceiver, 
    	          new IntentFilter(BluetoothDevice.ACTION_FOUND));

    	registerReceiver(broadcastReceiver, 
    	          new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

    	Log.i("BluetoothService", "Started discovery");

    	Boolean runningBluetooth = adapter.startDiscovery();

    	if (!runningBluetooth) {
    		unregisterReceiver(broadcastReceiver);
    		CharSequence text = "Sorry, bluetooth is off!";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
    	}
    }
    
    

}
