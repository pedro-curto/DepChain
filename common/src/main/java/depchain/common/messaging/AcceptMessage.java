package depchain.common.messaging;

public class AcceptMessage extends Message {

	private String value;

	public AcceptMessage(String value) {
		super(MessageType.ACCEPT);
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
