package depchain.common.domain;

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

	@Override
	public String toString() {
		return "ValueTimestampPair{" +
				"timestamp=" + timestamp +
				", value='" + value + '\'' +
				'}';
	}
}
