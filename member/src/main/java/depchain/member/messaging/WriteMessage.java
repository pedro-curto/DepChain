package depchain.member.messaging;

import depchain.common.messaging.Message;
import depchain.member.domain.ValueTimestampPair;

public class WriteMessage extends Message {

	private ValueTimestampPair valts;

	public WriteMessage(ValueTimestampPair valts) {
		super(MessageType.WRITE);
		this.valts = valts;
	}
}
