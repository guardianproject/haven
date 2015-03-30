/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.messages;

import java.util.Date;

public class KeyRequest extends BluetoothMessageData implements BluetoothMessage {
	
	private double lat;
	private double lng;
	
	public KeyRequest(double lat, double lng, Date timestamp) {
		this.lat = lat; 
		this.lng = lng;
		this.timestamp = timestamp;
	}
	
	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5241265795662117718L;

	public MessageType getType() {
		return MessageType.KEY_REQUEST;
	}

}
