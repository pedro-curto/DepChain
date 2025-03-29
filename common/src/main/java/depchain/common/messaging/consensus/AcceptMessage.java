package depchain.common.messaging.consensus;

import depchain.common.messaging.Message;

public class AcceptMessage extends Message {

	private String value;
	private final int consensusInstance;

	public AcceptMessage(String value, int port, int consensusInstance) {
		super(MessageType.ACCEPT, port);
		this.value = value;
		this.consensusInstance = consensusInstance;
	}

	public String getValue() {
		return value;
	}

	public int getConsensusInstance() {
		return consensusInstance;
	}

	@Override
	public String toString() {
		return "ACCEPT(" + this.value + "), " + consensusInstance;
	}
}
