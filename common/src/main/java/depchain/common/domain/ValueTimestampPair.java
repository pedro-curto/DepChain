package depchain.common.domain;

import java.util.Objects;

public class ValueTimestampPair {

	private int timestamp;
	private String value;
	private String clientSignature;
	private int clientPort;
	private long nonce;

	public ValueTimestampPair(int timestamp, String value, int clientPort, long nonce) {
		this.value = value;
		this.timestamp = timestamp;
		this.clientPort = clientPort;
		this.nonce = nonce;
	}

	public ValueTimestampPair(int epoch, String value) {
		this.timestamp = epoch;
		this.value = value;
	}

	public int getClientPort() {
		return clientPort;
	}

	public long getNonce() {
		return nonce;
	}

	//public void setClientName(String clientName) {
	//	this.clientName = clientName;
	//}

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
		return "<" + timestamp + "," + value + "," + clientPort + "," + nonce + ">";
	}

	public void setClientSignature(String signature) {
		this.clientSignature = signature;
	}

	public String getClientSignature() { return clientSignature; }

	// we need an equals and hashcode to be able to group VTPs in the write and accept phases
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || o.getClass() != this.getClass()) return false;
		ValueTimestampPair other = (ValueTimestampPair) o;
		return this.value.equals(other.value) && this.timestamp == other.timestamp
				&& this.clientPort == other.clientPort && this.nonce == other.nonce;
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, value, clientPort, nonce);
	}

}
