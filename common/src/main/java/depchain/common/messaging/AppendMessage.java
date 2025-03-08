package depchain.common.messaging;

public class AppendMessage extends Message {

	private final String value;

	public AppendMessage(String value) {
		super(MessageType.APPEND);
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String getHmacData() {
		return super.getHmacData() + value;
	}
}
