package depchain.common.messaging;

public class AcceptMessage extends Message {

	private String value;

	public AcceptMessage(String value, int port) {
		super(MessageType.ACCEPT, port);
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + this.value + ")";
	}
}
