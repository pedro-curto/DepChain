package depchain.common.messaging;

import depchain.common.domain.ConsensusState;

import java.util.ArrayList;

public class CollectedMessage extends Message {

	private ArrayList<ConsensusState> states;

	public CollectedMessage(ArrayList<ConsensusState> states) {
		super(MessageType.COLLECTED);
		this.states = states;
	}
}
