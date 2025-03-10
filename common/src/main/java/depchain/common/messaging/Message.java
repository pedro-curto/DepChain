package depchain.common.messaging;

public class Message {

	private long sequenceNumber;
	private final MessageType type;
	private String hmac = null;
	private int port = -1; // used as id since all processes are running in localhost
	//TODO -> use (address + port) or an id to identify processes as it is more correct

	public enum MessageType {
		APPEND,
		ACK,
		CLIENT_REPLY,
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

	public Message(MessageType messageType, int port) {
		this.type = messageType;
		this.port = port;
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

	public void setPort(int port) {
		this.port = port;
	}
	public int getPort() {
		return port;
	}

	public String getHmacData() {
		return "" + sequenceNumber + type;
	}

	@Override
	public String toString() {
		return sequenceNumber + "||" + type;
	}
}
