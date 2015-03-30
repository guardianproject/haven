/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.bluetooth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.bluetooth.BluetoothSocket;

public class ObjectBluetoothSocket {
	
	/**
	 * Original byte oriented socket
	 */
	private BluetoothSocket socket;
	
	/**
	 * Object output stream associated with socket
	 */
	private ObjectOutputStream outputStream;
	
	/**
	 * Object input stream associated with socket
	 */
	private ObjectInputStream inputStream;
	
	public ObjectBluetoothSocket(BluetoothSocket socket) throws IOException {
		this.socket = socket;
		this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
		this.inputStream = new ObjectInputStream(this.socket.getInputStream());
	}
	
	public ObjectInputStream getInputStream() {
		return inputStream;
	}
	
	public ObjectOutputStream getOutputStream() {
		return outputStream;
	}
	
	public void close() throws IOException {
		outputStream.close();
		inputStream.close();
		socket.close();
	}
	
	public void connect() throws IOException {
		socket.connect();
	}
	

}
