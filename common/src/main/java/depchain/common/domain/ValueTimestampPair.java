package depchain.common.domain;

public class ValueTimestampPair {

	private int timestamp;

	private final String value;
	private String clientSignature;
	private String clientName;
	private long nonce;

	public ValueTimestampPair(int timestamp, String value, String clientName, long nonce) {
		this.value = value;
		this.timestamp = timestamp;
		this.clientName = clientName;
		this.nonce = nonce;
	}

	public ValueTimestampPair(int epoch, String value) {
		this.timestamp = epoch;
		this.value = value;
	}

	public String getClientName() {
		return clientName;
	}

	public long getNonce() {
		return nonce;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public int getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "<" + timestamp + "," + value + "," + clientName + ">";
	}

	public void setClientSignature(String signature) {
		this.clientSignature = signature;
	}

	public String getClientSignature() { return clientSignature; }
}
