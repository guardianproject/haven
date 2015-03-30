/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.config;

import java.util.UUID;

public class Remote {
	
	public static final String HOST = "http://10.192.6.90:9000/";
	
	public static final String ACCESS_TOKEN = "users/accesstoken";
	
	public static final String PHONES = "api/phones";
	
	public static final String UPLOAD_IMAGES = "/image";
			
	public static final String UPLOAD_AUDIO = "/audio";
	
	public static final String UPLOAD_POSITION = "/position";
	
	public static final String DELEGATED_UPLOAD_POSITION = "/position/delegated";
	
	public static final String BLUETOOTH_NAME = "me.ziccard.secureit";
	
	public static final UUID BLUETOOTH_UUID =
		      UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
}
