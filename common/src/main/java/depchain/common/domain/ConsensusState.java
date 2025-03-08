package depchain.common.domain;

import java.util.ArrayList;
import java.util.List;

public class ConsensusState {

	private String memberName;

	private ValueTimestampPair current;

	private List<ValueTimestampPair> writeset;

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

	@Override
	public String toString() {
		return "ConsensusState{" +
				"memberName='" + memberName + '\'' +
				", current=" + current +
				", writeset=" + writeset +
				'}';
	}
}
