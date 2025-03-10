package depchain.common.messaging;

import java.util.List;

public class CollectedMessage extends Message {

	private List<StateMessage> states;

	public CollectedMessage(List<StateMessage> states) {
		super(MessageType.COLLECTED);
		this.states = states;
	}

	public List<StateMessage> getStates() {
		return states;
	}

	@Override
	public String toString() {
		return "CollectedMessage{" +
				"states=" + states +
				'}';
	}
}
