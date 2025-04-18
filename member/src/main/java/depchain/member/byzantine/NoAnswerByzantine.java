package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.domain.ConsensusState;
import depchain.common.messaging.*;
import depchain.common.messaging.consensus.*;
import depchain.common.messaging.library.AppendMessage;
import depchain.member.domain.Config;
import depchain.member.domain.Member;
import depchain.member.state.StringChain;

import java.util.concurrent.BlockingQueue;

public class NoAnswerByzantine extends Member {

    public NoAnswerByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, StringChain bcState, BlockingQueue<Message> messageQueue) {
        super(config, dcLogger, pf, cState, bcState, messageQueue);
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
}
