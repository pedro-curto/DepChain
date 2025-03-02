package depchain.common;

public class Message {
	private String msgId;
	private String senderId;
	private String msgContent;
	private String signature;
	private String msgType;

	public Message(String msgId, String senderId, String msgContent, String signature, String msgType) {
		this.msgId = msgId;
		this.senderId = senderId;
		this.msgContent = msgContent;
		this.signature = signature;
		this.msgType = msgType;
	}

	public String getMsgId() {
		return msgId;
	}

	public String getSenderId() {
		return senderId;
	}

	public String getMsgContent() {
		return msgContent;
	}

	public String getSignature() {
		return signature;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	@Override
	public String toString() {
		return msgId + "||" + senderId + "||" + msgContent + "||" + signature + "||" + msgType;
	}
}
