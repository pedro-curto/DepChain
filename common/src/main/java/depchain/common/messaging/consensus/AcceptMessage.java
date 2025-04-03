package depchain.common.messaging.consensus;

import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

public class AcceptMessage extends Message {

	//private String value;
	private ValueTimestampPair valts;
	private final int consensusInstance;

	public AcceptMessage(ValueTimestampPair valts, int port, int consensusInstance) {
		super(MessageType.ACCEPT, port);
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
		return "ACCEPT(" + this.valts + "), " + consensusInstance;
	}
}
