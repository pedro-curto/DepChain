package depchain.member.messaging;

import depchain.common.messaging.Message;
import depchain.member.domain.ConsensusState;

import java.util.ArrayList;

public class CollectedMessage extends Message {

	private ArrayList<ConsensusState> states;

	public CollectedMessage(ArrayList<ConsensusState> states) {
		super(MessageType.COLLECTED);
		this.states = states;
	}
}
