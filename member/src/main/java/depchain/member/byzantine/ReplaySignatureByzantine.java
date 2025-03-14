package depchain.member.byzantine;

import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.messaging.CollectedMessage;
import depchain.common.messaging.ReadMessage;
import depchain.common.messaging.StateMessage;
import depchain.member.domain.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReplaySignatureByzantine extends Member {

    private static final Random random = new Random();

    // Stores State messages from other members and replays them
    private List<StateMessage> membersStateMessages = new ArrayList<>();

    public ReplaySignatureByzantine(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
        super(memberName, members, clients, port, address, debug);
        System.out.println("ReplaySignatureByzantine started at port " + port);
    }

    @Override
    public void handleCollected(CollectedMessage collectedMessage) {
        dcLogger.log("Received: " + collectedMessage);
        if (collectedMessage.getPort() != leader.getPort()) {
            return;
        }
        consensusState.addCollectedMessage(collectedMessage);

        // Stores other members state messages to replay
        this.membersStateMessages = new ArrayList<>(collectedMessage.getStates());
    }

    @Override
    public void handleRead(ReadMessage readMessage) {
        if (this.membersStateMessages.isEmpty()) {
            // No states received yet: behave normally
            super.handleRead(readMessage);
        } else {
            dcLogger.log("Received: " + readMessage);
            // Use random state message received in previous epoch
            StateMessage stateMessage = this.membersStateMessages.get(random.nextInt(this.membersStateMessages.size()));
            stateMessage.setConsensusInstance(readMessage.getConsensusInstance());
            dcLogger.log("Sending fake state message pretending to be " + stateMessage.getState().getMemberName() + " -> " + stateMessage);
            sendToLeader(stateMessage);
        }
    }


}
