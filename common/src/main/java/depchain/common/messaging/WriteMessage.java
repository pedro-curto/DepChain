package depchain.common.messaging;

import depchain.common.domain.ValueTimestampPair;

public class WriteMessage extends Message {

	private ValueTimestampPair valts;

	public WriteMessage(ValueTimestampPair valts) {
		super(MessageType.WRITE);
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
