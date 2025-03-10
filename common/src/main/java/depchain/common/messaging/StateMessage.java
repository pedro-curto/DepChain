package depchain.common.messaging;

import depchain.common.domain.ConsensusState;

public class StateMessage extends Message {

	private ConsensusState state;
	private String signature;

	public StateMessage(ConsensusState state, String signature) {
		super(MessageType.STATE);
		this.state = state;
		this.signature = signature;
	}

	public ConsensusState getState() {
		return state;
	}

	public String getSignature() {
		return signature;
	}

	@Override
	public String toString() {
		return "StateMessage{" +
				"state=" + state +
				", signature='" + signature + '\'' +
				'}';
	}
}
