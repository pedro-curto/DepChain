package depchain.common.messaging;

import depchain.common.domain.ConsensusState;

public class StateMessage extends Message {

	private ConsensusState state;
	private String signature;
	private int consensusInstance;

	public StateMessage(ConsensusState state, String signature, int consensusInstance, int port) {
		super(MessageType.STATE, port);
		this.state = state;
		this.signature = signature;
		this.consensusInstance = consensusInstance;
	}

	public ConsensusState getState() {
		return state;
	}

	public String getSignature() {
		return signature;
	}

	public void setConsensusInstance(int consensusInstance) {
		this.consensusInstance = consensusInstance;
	}

	public int getConsensusInstance() {
		return consensusInstance;
	}

	@Override
	public String toString() {
		return state.toString() + ", Instance: " + consensusInstance;
	}
}
