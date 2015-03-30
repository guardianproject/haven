/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async.upload;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

import me.ziccard.secureit.SecureItPreferences;
import me.ziccard.secureit.bluetooth.ObjectBluetoothSocket;
import me.ziccard.secureit.config.Remote;
import me.ziccard.secureit.messages.BluetoothMessage;
import me.ziccard.secureit.messages.KeyRequest;
import me.ziccard.secureit.messages.MessageBuilder;
import me.ziccard.secureit.messages.MessageType;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class BluetoothPeriodicPositionUploaderTask extends AsyncTask<Void, Void, Void> {
	
	private Context context;
	
	/**
	 * Boolean true iff last thread iterations position has been sent
	 */
	private boolean dataSent = false;
	
	private SecureItPreferences prefs;
		
	/**
	 * Adapter for bluetooth services
	 */
	private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	
	/**
	 * Creates a BroadcastReceiver for ACTION_FOUND and ACTION_DISCOVERY_FINISHED
	 */
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

	            Log.i("BluetoothPeriodicPositionUploaderTask", "Discovered "+device.getName());
	    		CharSequence text = "Discovered "+device.getName();
	    		int duration = Toast.LENGTH_SHORT;
	    		Toast toast = Toast.makeText(context, text, duration);
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
		            		
		            		Log.i("BluetoothPeriodicPositionUploaderTask", "Trying to connect to " + device.getName());
		            		
		            		adapter.cancelDiscovery();
		            		tmp.connect();
		            		
		            		Log.i("BluetoothPeriodicPositionUploaderTask", "Connected to " + device.getName());
		            		
		            		ObjectBluetoothSocket socket = new ObjectBluetoothSocket(tmp);    		           		
		            		
		            		MessageBuilder builder = new MessageBuilder();
		            		builder.setPhoneId(phoneId);
		            		builder.setTimestamp(new Date());
		            		//Sending hello message
		            		BluetoothMessage message = builder.buildMessage(MessageType.HELLO);			    	
		   			    	ObjectOutputStream ostream = socket.getOutputStream();
		   			    	ostream.writeObject(message);
		   			    	Log.i("BluetoothPeriodicPositionUploaderTask", "Sent message " + message.getType().toString());
		   			    	
		   			    	//Receiving key request
		   			    	ObjectInputStream instream = socket.getInputStream();
		   			    	KeyRequest keyRequestMessage = (KeyRequest) instream.readObject();
		   			    	Log.i("BluetoothPeriodicPositionUploaderTask", "Received message "+keyRequestMessage.getType().toString()); 
		   			    	
		   			    	builder.setLat(keyRequestMessage.getLat());
		   			    	builder.setLng(keyRequestMessage.getLng());
		   			    	builder.setTimestamp(keyRequestMessage.getTimestamp());
		   			    	builder.setPassword(delegatedAccessToken);
		   			    	
		   			    	Log.i("BluetoothPeriodicPositionUploaderTask",delegatedAccessToken);
		   			    	
		   			    	message = builder.buildMessage(MessageType.KEY_RESPONSE);
		   			    	ostream.writeObject(message);
		   			    	Log.i("BluetoothPeriodicPositionUploaderTask", "Sent message " + message.getType().toString());
		   			    	
		   			    	//Reading last object
		   			    	instream.readObject();
		   			    	
		   			    	socket.close();
		            			            		
		            		CharSequence text = "Bluetooth message sent";
		        			int duration = Toast.LENGTH_SHORT;
		        			Toast toast = Toast.makeText(context, text, duration);
		        			toast.show();  	
		        			
		        			dataSent = true;
		            	}
					} catch (Exception e) {
						e.printStackTrace();
					}
	           }	        	
	        	context.unregisterReceiver(broadcastReceiver);
	        }
	    }
	};
	
	/**
	 * ID of the phone sending data
	 */
	private String phoneId = null;
	
	/**
	 * Access token of the phone sending data
	 */
	private String delegatedAccessToken = null;

	/**
	 * Constructor
	 * @param context
	 * @param lat
	 * @param lng
	 */
	public BluetoothPeriodicPositionUploaderTask(Context context) {
		this.context = context;
		prefs = new SecureItPreferences(context);			
		this.phoneId = prefs.getPhoneId();	
		this.delegatedAccessToken = prefs.getDelegatedAccessToken();
		
		Log.i("BluetoothPeriodicPositionUploaderTask", "Created");
	}
	
	@Override
	protected void onCancelled () {
		Log.i("BluetoothPeriodicPositionUploaderTask", "Task stopped");
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		
		while(true) {
			
			this.context.registerReceiver(broadcastReceiver, 
		  	        new IntentFilter(BluetoothDevice.ACTION_FOUND));

			this.context.registerReceiver(broadcastReceiver, 
					new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
			
	    	Boolean runningBluetooth = adapter.startDiscovery();

	    	if (!runningBluetooth) {
	    		Log.i("BluetoothPeriodicPositionUploaderTask", "Bluetooth is off");
	    	} else {
	    		Log.i("BluetoothPeriodicPositionUploaderTask", "Started discovery");	    		
	    	}
	    				
			if (isCancelled()) {
				return null;
			}
						
		    try {
		    	if (dataSent) 	Thread.sleep(3600*1000);
		    	else			Thread.sleep(60*1000);
			} catch (InterruptedException e) {
				return null;
			}
		}
	}
}