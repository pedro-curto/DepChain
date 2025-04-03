package depchain.common.messaging.consensus;

import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

public class ReadMessage extends Message {

	private final int consensusInstance;

	public ReadMessage(int consensusInstance) {
		super(MessageType.READ);
		this.consensusInstance = consensusInstance;
	}

	public int getConsensusInstance() {
		return consensusInstance;
	}

	@Override
	public String toString() {
		return "READ(), Instance: " + consensusInstance;
	}
}
