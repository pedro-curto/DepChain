package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.messaging.library.AppendMessage;
import depchain.common.messaging.Message;
import depchain.common.messaging.consensus.ReadMessage;
import depchain.common.messaging.consensus.StateMessage;
import depchain.member.domain.Config;
import depchain.member.domain.Member;
import depchain.member.state.StringChain;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class FakeSignatureByzantine extends Member {

    private static final Random random = new Random();

    public FakeSignatureByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, StringChain bcState, BlockingQueue<Message> messageQueue) {
        super(config, dcLogger, pf, cState, bcState, messageQueue);
    }


    public String getRandomMemberName() {
        String memberName;
        do {
            memberName = config.getMembers().get(random.nextInt(config.getMembers().size())).getEntityName();
        } while(memberName.equals(config.getMyName()));

        return memberName;
    }

    public int getMemberPort(String name) {
        for (Entity entity : config.getMembers()) {
            if (entity.getEntityName().equals(name)) {
                return entity.getPort();
            }
        }
        dcLogger.error("COULDN'T FIND MEMBER");
        return -1;
    }

    @Override
    public void handleRead(ReadMessage readMessage) {
        dcLogger.log("Received: " + readMessage);
        ConsensusState consensusState = consensusHandler.getConsensusState();
        String dataToSign = consensusState.getCurrent().toString() + consensusState.getWriteset();
        String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(config.getMyName()));

        String randomMemberName = getRandomMemberName();
        // Send as a another member name
        ConsensusState myState = new ConsensusState(randomMemberName, consensusState.getCurrent(), consensusState.getWriteset());
        StateMessage stateMessage = new StateMessage(myState, mySignature, consensusState.getInstance(), getMemberPort(randomMemberName));
        dcLogger.log("Sending fake state message... : " + stateMessage);
        sendToLeader(stateMessage);
    }
}
