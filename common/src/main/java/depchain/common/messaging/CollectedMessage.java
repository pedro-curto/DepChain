package depchain.common.messaging;

import java.util.List;

public class CollectedMessage extends Message {

	private List<StateMessage> states;
	private final int consensusInstance;

	public CollectedMessage(List<StateMessage> states, int port, int consensusInstance) {
		super(MessageType.COLLECTED, port);
		this.states = states;
		this.consensusInstance = consensusInstance;
	}

	public List<StateMessage> getStates() {
		return states;
	}

	public int getConsensusInstance() {
		return consensusInstance;
	}

	@Override
	public String toString() {
		return "COLLECTED(" + states + "), Instance: " + consensusInstance;
	}
}
