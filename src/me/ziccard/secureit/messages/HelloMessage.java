package me.ziccard.secureit.messages;

public class HelloMessage extends BluetoothMessageData implements BluetoothMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4363335604763136855L;

	public MessageType getType() {
		return MessageType.HELLO;
	}

}
