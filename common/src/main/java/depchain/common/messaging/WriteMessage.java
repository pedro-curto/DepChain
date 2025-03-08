package depchain.common.messaging;

import depchain.common.domain.ValueTimestampPair;

public class WriteMessage extends Message {

	private ValueTimestampPair valts;

	public WriteMessage(ValueTimestampPair valts) {
		super(MessageType.WRITE);
		this.valts = valts;
	}
}
