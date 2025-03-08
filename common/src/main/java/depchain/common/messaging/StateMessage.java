package depchain.common.messaging;

import depchain.common.domain.ConsensusState;

public class StateMessage extends Message {

	private ConsensusState state;

	public StateMessage(ConsensusState state) {
		super(MessageType.STATE);
		this.state = state;
	}

	public ConsensusState getState() {
		return state;
	}

	@Override
	public String toString() {
		return "StateMessage{" +
				"state=" + state +
				'}';
	}
}
