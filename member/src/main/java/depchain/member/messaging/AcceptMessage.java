package depchain.member.messaging;

import depchain.common.messaging.Message;

public class AcceptMessage extends Message {

	private String value;

	public AcceptMessage(String value) {
		super(MessageType.ACCEPT);
		this.value = value;
	}
}
