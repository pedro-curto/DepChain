package depchain.common.session;

import java.util.Objects;

public class SessionTaskKey {
	private int sessionId;
	private long sequenceNumber;

	public SessionTaskKey(int sessionId, long sequenceNumber) {
		this.sessionId = sessionId;
		this.sequenceNumber = sequenceNumber;
	}

	public int getSessionId() {
		return sessionId;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SessionTaskKey) {
			SessionTaskKey other = (SessionTaskKey) obj;
			return sessionId == other.sessionId && sequenceNumber == other.sequenceNumber;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sessionId, sequenceNumber);
	}

	@Override
	public String toString() {
		return "SessionTaskKey [sessionId=" + sessionId + ", sequenceNumber=" + sequenceNumber + "]";
	}

}
