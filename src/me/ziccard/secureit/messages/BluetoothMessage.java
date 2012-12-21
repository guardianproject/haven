package me.ziccard.secureit.messages;

import java.io.Serializable;

public interface BluetoothMessage extends Serializable {
	
	public MessageType getType();
	
}
