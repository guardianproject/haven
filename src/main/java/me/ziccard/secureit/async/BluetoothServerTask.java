/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import me.ziccard.secureit.async.upload.DelegatedPositionUploaderTask;
import me.ziccard.secureit.bluetooth.ObjectBluetoothSocket;
import me.ziccard.secureit.config.Remote;
import me.ziccard.secureit.messages.HelloMessage;
import me.ziccard.secureit.messages.KeyRequest;
import me.ziccard.secureit.messages.KeyResponse;
import me.ziccard.secureit.messages.MessageBuilder;
import me.ziccard.secureit.messages.MessageType;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class BluetoothServerTask extends Thread {
	
	public class NoBluetoothException extends Exception {

		private static final long serialVersionUID = 4817843408138269806L;
		
	}
	
    private final BluetoothServerSocket mmServerSocket;
    
    private BluetoothAdapter mBluetoothAdapter;
    
    private Context context;
 
    public BluetoothServerTask(Context context) throws NoBluetoothException {
    	
    	Log.i("BluetoothServerTask", "Created");
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	if (mBluetoothAdapter == null) {
    	    // Device does not support Bluetooth
    		throw new NoBluetoothException();
    		
    	}
    	this.context = context;
    	Log.i("BluetoothServerTask", "Got adapter");
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
            		Remote.BLUETOOTH_NAME, 
            		Remote.BLUETOOTH_UUID);
            Log.i("BluetoothServerTask", "Created insecure server socket");
        } catch (IOException e) { 
        	throw new NoBluetoothException();
        }
        mmServerSocket = tmp;
    }
 
    public void run() {
        ObjectBluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
            	Log.i("BluetoothServerTask", "Waiting for a connection");

            	BluetoothSocket tmp = mmServerSocket.accept();
            	      	
                socket = new ObjectBluetoothSocket(tmp);
                Log.i("BluetoothServerTask", "Server creted a socket");
                
            } catch (IOException e) {
            	Log.i("AcceptException", e.getStackTrace().toString());
            	e.printStackTrace();
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket(socket);
            }
        }
    }
 
    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }
    
    /**
     * Manage the new connection
     */
    public void manageConnectedSocket(final ObjectBluetoothSocket socket) {
    	Log.i("BluetoothServerTask", "Connection accepted");
    	(new Thread() {
    		@Override
    		public void run() {    
    			try {
    				//Receiving first hello message
    				ObjectInputStream instream = socket.getInputStream();
   			    	HelloMessage message = (HelloMessage) instream.readObject();
   			    	Log.i("BluetoothServerTask", "Received message "+message.getType().toString());
   			    	
   			    	/*
   			    	 * Get last known location
   			    	 */
   			    	LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
   			    	String locationProvider = LocationManager.NETWORK_PROVIDER;
   			    	Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
   			    	
   			    	//Sending key request
   			    	ObjectOutputStream ostream = socket.getOutputStream();
   			    	MessageBuilder builder = new MessageBuilder();
   			    	builder.setLat(lastKnownLocation.getLatitude());
   			    	builder.setLng(lastKnownLocation.getLongitude());
   			    	builder.setTimestamp(new Date());
   			    	
   			    	KeyRequest keyRequest = (KeyRequest) builder.buildMessage(MessageType.KEY_REQUEST);
   			    	ostream.writeObject(keyRequest);
   			    	Log.i("BluetoothServerTask", "Sent message " + keyRequest.getType().toString());
   			    	   			    	
   			    	//Receiving key response
   			    	KeyResponse keyResponse = (KeyResponse) instream.readObject();
   			    	Log.i("BluetoothServerTask", "Received message "+keyResponse.getType().toString());
   			    	
   			    	ostream.writeObject(new String("STOP"));
   			    	
   			    	new DelegatedPositionUploaderTask(
   			    			message.getPhoneId(), 
   			    			keyRequest.getLat(), 
   			    			keyRequest.getLng(), 
   			    			keyResponse.getAccessKey()).execute();
   			    	
   			    	socket.close();
    			} catch (Exception e) {
    			    Log.d("BLUETOOTH_COMMS", e.getMessage());
    			}
    		}
    	}).start();
    }
}