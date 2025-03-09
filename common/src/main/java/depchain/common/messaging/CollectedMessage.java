package depchain.common.messaging;

import depchain.common.domain.ConsensusState;

import java.util.ArrayList;
import java.util.List;

public class CollectedMessage extends Message {

	private List<ConsensusState> states;

	public CollectedMessage(List<ConsensusState> states) {
		super(MessageType.COLLECTED);
		this.states = states;
	}

	@Override
	public String toString() {
		return "CollectedMessage{" +
				"states=" + states +
				'}';
	}
}
