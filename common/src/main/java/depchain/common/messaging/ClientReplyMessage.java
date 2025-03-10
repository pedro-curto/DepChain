package depchain.common.messaging;

public class ClientReplyMessage extends Message {
	private String value;
	private boolean success;
	private int instanceOfDecision;

	public ClientReplyMessage(String value, boolean success, int instanceOfDecision) {
		super(MessageType.CLIENT_REPLY);
		this.value = value;
		this.success = success;
		this.instanceOfDecision = instanceOfDecision;
	}

	public String getValue() {
		return value;
	}

	public boolean getSuccess() {
		return success;
	}

	public int getInstanceOfDecision() {
		return instanceOfDecision;
	}

	@Override
	public String toString() {
		return "ClientReply{" +
				"value='" + value + '\'' +
				"success='" + success + '\'' +
				", instanceOfDecision='" + instanceOfDecision + '\'' +
				'}';
	}
}
