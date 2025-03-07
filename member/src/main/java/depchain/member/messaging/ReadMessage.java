package depchain.member.messaging;

import depchain.common.messaging.Message;

public class ReadMessage extends Message {

	public ReadMessage() {
		super(MessageType.READ);
	}
}
