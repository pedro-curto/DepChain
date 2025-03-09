package depchain.member.domain;

import depchain.common.domain.ConsensusState;

import java.util.ArrayList;
import java.util.List;

public class ConsensusLeaderState extends ConsensusState {
	private Object lock = new Object();
	// Store member states for collected message
	private List<ConsensusState> memberStates = new ArrayList<>();

	public ConsensusLeaderState(String leaderName) {
		super(leaderName);
	}

	public void addMemberState(ConsensusState state) {
		synchronized (lock) {
			memberStates.add(state);
			lock.notifyAll();
		}
	}

	public List<ConsensusState> waitForQuorum(int quorumSize) {
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

	public List<ConsensusState> getMemberStates() {
		return memberStates;
	}


}
