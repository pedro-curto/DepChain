package depchain.common.messaging;

public class Message {

	private long sequenceNumber;
	private final MessageType type;
	private String hmac = null;

	public enum MessageType {
		APPEND,
		ACK,
		KEY_EXCHANGE,
		READ,
		WRITE,
		STATE,
		COLLECTED,
		ACCEPT,
	}

	public Message(long sequenceNumber, MessageType messageType) {
		this.sequenceNumber = sequenceNumber;
		this.type = messageType;
	}

	public Message(MessageType type) {
		this.type = type;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public String getHmac() { return hmac; }
	public void setHmac(String hmac) { this.hmac = hmac; }

	public MessageType getType() {
		return type;
	}

	public String getHmacData() {
		return "" + sequenceNumber + type;
	}

	@Override
	public String toString() {
		return sequenceNumber + "||" + type;
	}
}
