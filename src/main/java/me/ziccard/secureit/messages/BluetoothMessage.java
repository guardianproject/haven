/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.messages;

import java.io.Serializable;

public interface BluetoothMessage extends Serializable {
	
	public MessageType getType();
	
}
