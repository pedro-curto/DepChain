package depchain.common.messaging;

public class AppendMessage extends Message {

	private final String value;
	private String signature;

	public AppendMessage(String value, int port) {
		super(MessageType.APPEND, port);
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setSignature(String signature) { this.signature = signature; }
	public String getSignature() { return signature; }

	/** Processes other than the receiving member (that verifies integrity with hmac)
	* should be able to check the origin of the request
	* */
	public String getDataToSign() {
		return value;
	}

	@Override
	public String getHmacData() {
		return super.getHmacData() + value + signature;
	}

	@Override
	public String toString() {
		return "APPEND(" + value + ")";
	}
}
