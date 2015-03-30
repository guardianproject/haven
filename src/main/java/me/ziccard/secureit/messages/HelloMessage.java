/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.messages;

import java.util.Date;

public class HelloMessage extends BluetoothMessageData implements BluetoothMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4363335604763136855L;
	
	/**
	 * Id of the phone delegating an alert
	 */
	private String phoneId;
	
	/**
	 * Constructor
	 */
	public HelloMessage(String phoneId, Date timestamp) {
		this.phoneId = phoneId;
		this.timestamp = timestamp;
	}
	
	/**
	 * phoneId getter
	 */
	public String getPhoneId() {
		return phoneId;
	}	

	public MessageType getType() {
		return MessageType.HELLO;
	}

}
