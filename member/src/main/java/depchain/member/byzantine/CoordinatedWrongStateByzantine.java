package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.*;
import depchain.member.domain.Config;
import depchain.member.domain.Member;
import depchain.member.state.BlockchainState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class CoordinatedWrongStateByzantine extends Member {

    // Always sends the same current (value,ts) and writset to try to win quorum in READ
    // Sends different write messages to different members (echos what he received from them)
    // in case 2 correct members have different values, could lead to a wrong quorum

    private boolean firstEpoch;
    private ValueTimestampPair valts;

    public CoordinatedWrongStateByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, BlockchainState bcState, BlockingQueue<Message> messageQueue, BlockingQueue<AppendMessage> appendQueue) {
        super(config, dcLogger, pf, cState, bcState, messageQueue, appendQueue);
        this.firstEpoch = true;
    }

    @Override
    public void handleRead(ReadMessage readMessage) {
        if (firstEpoch) {
            firstEpoch = false;
            super.handleRead(readMessage);
            return;
        }
        dcLogger.log("Received: " + readMessage);

        if(valts == null) {
            dcLogger.error("VALTS CANNOT BE NULL");
        }

        // Retrieves last value used, so it's signed by the client
        ValueTimestampPair fakeCurrent = new ValueTimestampPair(consensusState.getEpoch() + 1, valts.getValue());
        fakeCurrent.setClientSignature(valts.getClientSignature());

        // <"ola", 1> [<"ola", 0>]
        // Create new fake valts pair for writeset
        ValueTimestampPair fakeOld = new ValueTimestampPair(fakeCurrent.getTimestamp() - 1, fakeCurrent.getValue());
        fakeOld.setClientSignature(fakeCurrent.getClientSignature());
        ArrayList<ValueTimestampPair> fakeWriteset = new ArrayList<>();
        fakeWriteset.add(fakeOld);

        ConsensusState myState = new ConsensusState(config.getMyName(), fakeCurrent, fakeWriteset);
        String dataToSign = fakeCurrent.toString() + fakeWriteset;
        String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(config.getMyName()));

        myState.setInstance(consensusState.getInstance());
        StateMessage stateMessage = new StateMessage(myState, mySignature, consensusState.getInstance(), config.getPort());
        dcLogger.log("Faking state message... -> " + stateMessage);
        sendToLeader(stateMessage);
    }

    @Override
    public void handleCollected(CollectedMessage collectedMessage) {
        dcLogger.log("Received: " + collectedMessage);
        if (collectedMessage.getPort() != config.getLeader().getPort()) {
            return;
        }
        valts = collectedMessage.getStates().get(0).getState().getCurrent();
        consensusState.addCollectedMessage(collectedMessage);
    }

    @Override
    public void handleWrite(WriteMessage writeMessage) {
        dcLogger.log("Received: " + writeMessage);

        // Resending echo message to member
        WriteMessage echo = new WriteMessage(writeMessage.getValts(), config.getPort(), writeMessage.getConsensusInstance());
        sendToMember(echo, writeMessage.getPort());
        dcLogger.log("Sending fake echo WRITE message to " + writeMessage.getPort() + "... ");

        consensusState.addWriteMessage(writeMessage);

    }

    @Override
    public void handleAccept(AcceptMessage acceptMessage) {
        dcLogger.log("Received: " + acceptMessage);

        // Resending echo message to member
        AcceptMessage echo = new AcceptMessage(acceptMessage.getValue(), config.getPort(), acceptMessage.getConsensusInstance());
        sendToMember(echo, acceptMessage.getPort());
        dcLogger.log("Sending fake echo ACCEPT message to " + acceptMessage.getPort() + "... ");

        consensusState.addAcceptMessage(acceptMessage);
    }


    @Override
    public void broadCastMessage(Message message) {
        // Only send to me when broadcasting,
        // because it's echoing messages when receives from other members
        sendToMe(message);
    }

}
