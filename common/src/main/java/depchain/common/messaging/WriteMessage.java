package depchain.common.messaging;

import depchain.common.domain.ValueTimestampPair;

public class WriteMessage extends Message {

	private ValueTimestampPair valts;

	public WriteMessage(ValueTimestampPair valts, int port) {
		super(MessageType.WRITE, port);
		this.valts = valts;
	}

	public ValueTimestampPair getValts() {
		return valts;
	}

	@Override
	public String toString() {
		return "WriteMessage{" +
				"valts=" + valts +
				'}';
	}
}
