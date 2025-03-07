package depchain.common;

public class Message {

	private long sequenceNumber;
	private String senderName = null;
	private final String msgContent;
	private final String signature;
	private final MessageType type;

	public Message(long sequenceNumber, String msgContent, String signature, MessageType type) {
		this.sequenceNumber = sequenceNumber;
		this.msgContent = msgContent;
		this.signature = signature;
		this.type = type;
	}

	public Message(long sequenceNumber, String senderName, String msgContent, String signature, MessageType type) {
		this.sequenceNumber = sequenceNumber;
		this.senderName = senderName;
		this.msgContent = msgContent;
		this.signature = signature;
		this.type = type;
	}

	public Message(String senderName, String msgContent, String signature, MessageType type) {
		this.senderName = senderName;
		this.msgContent = msgContent;
		this.signature = signature;
		this.type = type;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public String getSenderName() {
		return senderName;
	}

	public String getMsgContent() {
		return msgContent;
	}

	public String getSignature() {
		return signature;
	}

	public MessageType getType() {
		return type;
	}

	@Override
	public String toString() {
		return sequenceNumber + "||" + senderName + "||" + msgContent + "||" + signature + "||" + type;
	}
}
