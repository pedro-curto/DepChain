package depchain.member.domain;

public class ValueTimestampPair {

	private int timestamp;

	private String value;

	public ValueTimestampPair(int timestamp, String value) {
		this.value = value;
		this.timestamp = timestamp;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public String getValue() {
		return value;
	}
}
