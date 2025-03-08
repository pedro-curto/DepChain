package depchain.member.domain;

import depchain.common.domain.ConsensusState;

import java.util.ArrayList;

public class ConsensusLeaderState extends ConsensusState {

	// Store member states for collected message
	private ArrayList<ConsensusState> memberStates = new ArrayList<>();

	public ConsensusLeaderState(String leaderName) {
		super(leaderName);
	}

	public void addMemberState(ConsensusState state) {
		memberStates.add(state);
	}
}
