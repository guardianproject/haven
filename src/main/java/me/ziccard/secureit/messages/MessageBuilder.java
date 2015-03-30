/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.messages;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

public class MessageBuilder {
	
	/**
	 * Phone Id
	 */
	private String phoneId;
	
	/**
	 * Latitude value
	 */
	private double lat;
	
	/**
	 * Longitude value	
	 */
	private double lng;
	
	/**
	 * Timestamp
	 */
	private Date timestamp;
	
	/**
	 * Password used to encrypt KeyResponse
	 */
	private String password;	
	
	public void setPhoneId(String phoneId) {
		this.phoneId = phoneId;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Constructor
	 * @param type
	 * @return
	 */	
	public BluetoothMessage buildMessage(MessageType type) {
		switch (type) {
		case HELLO:
			return new HelloMessage(phoneId, new Date());
		case KEY_REQUEST:
			return new KeyRequest(lat, lng, new Date());		
		case KEY_RESPONSE:
			String accessKey = "{\"lat\": \"" + lat + "\"," +
								"\"long\": \"" + lng + "\"," +	
								"\"timestamp\": \"" + timestamp + "\"}";

			SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES");
		    AlgorithmParameterSpec paramSpec = new IvParameterSpec(password.substring(0, 16).getBytes());
		    Cipher cipher;
			try {
				cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");	    
			    cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);			    	
			    byte[] ecrypted = cipher.doFinal(accessKey.getBytes());
			    accessKey = Base64.encodeToString(ecrypted, Base64.DEFAULT);
			} catch (Exception e) {
			
				e.printStackTrace();
			} 
			return new KeyResponse(accessKey, new Date());
		default:
			return new HelloMessage(phoneId, new Date());
		}
	}
}
