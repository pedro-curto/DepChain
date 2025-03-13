package depchain.member.byzantine;

import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.*;
import depchain.member.domain.Member;

import java.util.ArrayList;
import java.util.List;

public class CoordinatedWrongStateByzantine extends Member {

    // Always sends the same current (value,ts) and writset to try to win quorum in READ
    // Sends different write messages to different members (echos what he received from them)
    // in case 2 correct members have different values, could lead to a wrong quorum

    public CoordinatedWrongStateByzantine(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
        super(memberName, members, clients, port, address, debug);
    }

    @Override
    public void handleRead(ReadMessage readMessage) {
        dcLogger.log("Received: " + readMessage);

        ValueTimestampPair fakeCurrent = new ValueTimestampPair(1000, "Byzantine");
        ArrayList<ValueTimestampPair> fakeWriteset = new ArrayList<>();
        fakeWriteset.add(fakeCurrent);

        ConsensusState myState = new ConsensusState(myName, fakeCurrent, fakeWriteset);
        String dataToSign = fakeCurrent.toString() + fakeWriteset;
        String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(myName));

        // TODO -> i used the setter here to not change the constructor, use the constructor later
        myState.setInstance(consensusState.getInstance());
        StateMessage stateMessage = new StateMessage(myState, mySignature, consensusState.getInstance(), this.port);
        dcLogger.log("Faking state message... -> " + stateMessage);
        sendToLeader(stateMessage);
    }

    @Override
    public void handleWrite(WriteMessage writeMessage) {
        dcLogger.log("Received: " + writeMessage);

        // Resending echo message to member
        WriteMessage echo = new WriteMessage(writeMessage.getValts(), this.port, writeMessage.getConsensusInstance());
        sendToMember(echo, writeMessage.getPort());
        dcLogger.log("Sending fake echo WRITE message to " + writeMessage.getPort() + "... ");

        consensusState.addWriteMessage(writeMessage);
    }

    @Override
    public void handleAccept(AcceptMessage acceptMessage) {
        dcLogger.log("Received: " + acceptMessage);

        // Resending echo message to member
        AcceptMessage echo = new AcceptMessage(acceptMessage.getValue(), this.port, acceptMessage.getConsensusInstance());
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
