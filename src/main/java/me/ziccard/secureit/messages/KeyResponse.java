/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.messages;

import java.util.Date;

public class KeyResponse extends BluetoothMessageData implements BluetoothMessage {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4625933882220755290L;
	
	public KeyResponse(String accessKey, Date timestamp) {
		this.accessKey = accessKey;
		this.timestamp = timestamp;
	}
	
	private String accessKey;
	
	public String getAccessKey() {
		return accessKey;
	}

	public MessageType getType() {
		return MessageType.KEY_RESPONSE;
	}
}
