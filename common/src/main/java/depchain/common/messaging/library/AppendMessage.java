package depchain.common.messaging.library;

import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

public class AppendMessage extends Message {

	private final String value;
	private String signature;
	private long nonce;

	public AppendMessage(String value, int port, long nonce) {
		super(MessageType.APPEND, port);
		this.value = value;
		this.nonce = nonce;
	}

	public long getNonce() {
		return nonce;
	}

	public void setNonce(long nonce) {
		this.nonce = nonce;
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
		return value + nonce;
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
