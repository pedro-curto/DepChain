package depchain.member.domain;

import java.util.ArrayList;

public class ConsensusState {

	private String memberName;

	private ValueTimestampPair current;

	private ArrayList<ValueTimestampPair> writeset;

	public ConsensusState(String memberName) {
		// Initial State
		this.memberName = memberName;
		this.current = null;
		this.writeset = new ArrayList<>();
	}

	public ConsensusState(String memberName, ValueTimestampPair current, ArrayList<ValueTimestampPair> writeset) {
		this.memberName = memberName;
		this.current = current;
		this.writeset = writeset;
	}

	public boolean isInitialState() {
		return this.current == null;
	}
}
