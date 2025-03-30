package depchain.common.messaging.library;

import depchain.common.messaging.Message;

public class AllowanceMessage extends Message {
	private String owner;
	private String spender;

	public AllowanceMessage(String owner, String spender, int port) {
		super(MessageType.ALLOWANCE, port);
		this.owner = owner;
		this.spender = spender;
	}

	public String getOwner() {
		return owner;
	}

	public String getSpender() {
		return spender;
	}

	@Override
	public String toString() {
		return "AllowanceMessage{" +
				"owner='" + owner + '\'' +
				", spender='" + spender + '\'' +
				'}';
	}
}
