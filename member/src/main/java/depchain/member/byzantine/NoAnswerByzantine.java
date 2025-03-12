package depchain.member.byzantine;

import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.*;
import depchain.member.domain.ConsensusLeaderState;
import depchain.member.domain.Member;

import java.util.List;

public class NoAnswerByzantine extends Member {

    public NoAnswerByzantine(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
        super(memberName, members, clients, port, address, debug);
    }

    @Override
    public void handleRead(ReadMessage readMessage) {
        dcLogger.log("Received: " + readMessage);
        dcLogger.log("Ignoring...");
    }

    @Override
    public void handleState(StateMessage stateMessage) {
        dcLogger.log("Received: " + stateMessage);
        dcLogger.log("Ignoring...");
    }

    @Override
    public void handleCollected(CollectedMessage collectedMessage) {
        dcLogger.log("Received: " + collectedMessage);
        dcLogger.log("Ignoring...");
    }

    @Override
    public void handleWrite(WriteMessage writeMessage) {
        dcLogger.log("Received: " + writeMessage);
        dcLogger.log("Ignoring...");
    }

    @Override
    public void handleAccept(AcceptMessage acceptMessage) {
        dcLogger.log("Received: " + acceptMessage);
        dcLogger.log("Ignoring...");
    }

//    @Override
//    public void handleAppend(AppendMessage appendMessage) {
//        dcLogger.log("Received: " + appendMessage);
//        dcLogger.log("Ignoring...");
//    }

}
