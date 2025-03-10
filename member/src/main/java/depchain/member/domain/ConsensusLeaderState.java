package depchain.member.domain;

import depchain.common.domain.ConsensusState;
import depchain.common.messaging.StateMessage;

import java.util.ArrayList;
import java.util.List;

public class ConsensusLeaderState extends ConsensusState {
	private Object lock = new Object();
	// Store member states for collected message
	private List<StateMessage> memberStates = new ArrayList<>();

	public ConsensusLeaderState(String leaderName) {
		super(leaderName, 0);
	}

	public void addMemberState(StateMessage state) {
		synchronized (lock) {
			memberStates.add(state);
			lock.notifyAll();
		}
	}

	public List<StateMessage> waitForQuorum(int quorumSize) {
		synchronized (lock) {
			while (memberStates.size() < quorumSize) {
				try {
					System.out.println("Quorum not reached yet. Current size: " + memberStates.size());
					lock.wait();
				} catch (InterruptedException e) {
					System.out.println("Interrupted while waiting for quorum");
				}
			}
			return memberStates;
		}
	}

	public List<StateMessage> getMemberStates() {
		synchronized (lock) {
			return memberStates;
		}
	}


}
