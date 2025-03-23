package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.messaging.*;
import depchain.member.domain.Config;
import depchain.member.domain.Member;
import depchain.member.state.BlockchainState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class ReplaySignatureByzantine extends Member {

    private static final Random random = new Random();

    // Stores State messages from other members and replays them
    private List<StateMessage> membersStateMessages = new ArrayList<>();

    public ReplaySignatureByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, BlockchainState bcState, BlockingQueue<Message> messageQueue, BlockingQueue<AppendMessage> appendQueue) {
        super(config, dcLogger, pf, cState, bcState, messageQueue, appendQueue);
    }


    @Override
    public void handleCollected(CollectedMessage collectedMessage) {
        dcLogger.log("Received: " + collectedMessage);
        if (collectedMessage.getPort() != config.getLeader().getPort()) {
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
