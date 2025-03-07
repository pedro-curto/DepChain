package depchain.member.domain;

import java.util.ArrayList;

public class ConsensusLeaderState extends ConsensusState {

	// Store member states for collected message
	private ArrayList<ConsensusState> memberStates = new ArrayList<>();

	public ConsensusLeaderState(String leaderName) {
		super(leaderName);
	}
}
