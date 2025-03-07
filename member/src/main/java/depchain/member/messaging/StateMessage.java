package depchain.member.messaging;

import depchain.common.messaging.Message;
import depchain.member.domain.ConsensusState;

import java.util.ArrayList;

public class StateMessage extends Message {

	private ConsensusState state;

	public StateMessage(ConsensusState state) {
		super(MessageType.STATE);
		this.state = state;
	}
}
