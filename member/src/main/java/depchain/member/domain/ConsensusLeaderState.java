package depchain.member.domain;

import depchain.common.domain.ConsensusState;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.StateMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsensusLeaderState extends ConsensusState {
	private final Map<Integer, StateMessage> memberStates = new HashMap<>();
	private final Object statesLock = new Object();
	protected static final long STATE_TIMEOUT = 2000; // 2 seconds

	public ConsensusLeaderState(String leaderName, int consensusInstance) {
		super(leaderName, consensusInstance);
	}

	public void addMemberState(StateMessage state) {
		synchronized (statesLock) {
			if (memberStates.containsKey(state.getPort())) return;
			memberStates.put(state.getPort(), state);
		}
	}

	// TODO -> implement returning imediatlly after receiving a value that can be decided
	public List<StateMessage> waitForStateQuorum() {
		synchronized (statesLock) {
            try {
                statesLock.wait(STATE_TIMEOUT);
            } catch (InterruptedException e) {
				System.out.println("Interrupted while waiting for quorum");
			}
        	return new ArrayList<>(memberStates.values());
		}
	}

	@Override
	public void nextEpoch() {
		super.nextEpoch();
		memberStates.clear();
	}

	@Override
	public void nextInstance() {
		super.nextInstance();
		memberStates.clear();
	}
}
