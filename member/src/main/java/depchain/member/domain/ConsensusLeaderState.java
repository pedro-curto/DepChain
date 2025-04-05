package depchain.member.domain;

import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.messaging.consensus.StateMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.PublicKey;

public class ConsensusLeaderState extends ConsensusState {
	private final Map<Integer, StateMessage> memberStates = new HashMap<>();
	private final Object statesLock = new Object();
	protected static final long STATE_TIMEOUT = 2000; // 2 seconds
	private boolean caughtInvalidSignature = false;

	public ConsensusLeaderState(String leaderName, int consensusInstance) {
		super(leaderName, consensusInstance);
	}

	public void addMemberState(StateMessage state) {
		synchronized (statesLock) {
			// verify if the signature is valid, first
			if (!verifySignatureOfState(state)) {
				System.out.println("Caught an invalid signature from member " + state.getState().getMemberName());
				caughtInvalidSignature = true;
				return;
			}
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

	public boolean getCaughtInvalidSignature() {
		return caughtInvalidSignature;
	}

	public void setCaughtInvalidSignature(boolean caughtInvalidSignature) {
		this.caughtInvalidSignature = caughtInvalidSignature;
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

	@Override
	public String toString() {
		return "ConsensusLeaderState{" +
				"memberStates=" + memberStates +
				", statesLock=" + statesLock +
				", caughtInvalidSignature=" + caughtInvalidSignature +
				'}';
	}

	public boolean verifySignatureOfState(StateMessage stateMsg) {
		ConsensusState consensusSt = stateMsg.getState();
		PublicKey memberPubKey = Security.getMembershipPublicKey(consensusSt.getMemberName());
		String dataToSign = consensusSt.getDataToSign();
		//String dataToSign = consensusSt.getCurrent().toString() + consensusSt.getWriteset().toString() + consensusSt.getInstance() + consensusSt.getEpoch();
		return Security.verifyDS(stateMsg.getSignature(), dataToSign, memberPubKey);
	}
}
