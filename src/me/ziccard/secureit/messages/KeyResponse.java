package me.ziccard.secureit.messages;

public class KeyResponse extends BluetoothMessageData implements BluetoothMessage {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4625933882220755290L;
	
	private String accessKey;
	
	public String getAccessKey() {
		return accessKey;
	}

	public MessageType getType() {
		return MessageType.KEY_RESPONSE;
	}
}
