package depchain.common.messaging;

import depchain.common.domain.ValueTimestampPair;

public class WriteMessage extends Message {

	private ValueTimestampPair valts;
	private final int consensusInstance;

	public WriteMessage(ValueTimestampPair valts, int port, int consensusInstance) {
		super(MessageType.WRITE, port);
		this.valts = valts;
		this.consensusInstance = consensusInstance;
	}

	public ValueTimestampPair getValts() {
		return valts;
	}

	public int getConsensusInstance() {
		return consensusInstance;
	}

	@Override
	public String toString() {
		return "WRITE(" + valts + "), Instance: " + consensusInstance;
	}
}
