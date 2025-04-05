package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.*;
import depchain.common.messaging.consensus.AcceptMessage;
import depchain.common.messaging.consensus.StateMessage;
import depchain.common.messaging.consensus.WriteMessage;
import depchain.common.messaging.library.AppendMessage;
import depchain.member.domain.Config;
import depchain.member.domain.ConsensusHandler;
import depchain.member.domain.Member;
import depchain.member.state.StringChain;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class WrongWriteAcceptByzantine extends Member {

    ConsensusState consensusState;
    // Sends Write and Accept messages with the same byzantine value every time
    public WrongWriteAcceptByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, StringChain bcState, BlockingQueue<Message> messageQueue) {
        super(config, dcLogger, pf, cState, bcState, messageQueue);
        consensusState = super.consensusHandler.getConsensusState();
    }
//
//    // Byzantines send the same WriteMessage
//
//    public boolean decideValue() {
//        dcLogger.log("Waiting for states to decide value");
//        List<StateMessage> collectedStates = this.consensusState.waitForCollectedMessage();
//        if (collectedStates == null || collectedStates.size() < config.getByzantineQuorum()) {
//            // abort
//            dcLogger.log("aborted after collecting states");
//            this.consensusState.nextEpoch();
//            return false;
//        }
//        ValueTimestampPair value = consensusHandler.decideOnCollectedValues(collectedStates);
//        if (value == null) {
//            // abort
//            dcLogger.log("aborted after deciding null value");
//            this.consensusState.nextEpoch();
//            return false;
//        }
//        // Creates byzantine write, neglects decided value
//        ValueTimestampPair byzantinePair = new ValueTimestampPair(this.consensusState.getEpoch(), new ConsensusString("Byzantine"));
//        this.consensusState.updateWriteSet(byzantinePair);
//        WriteMessage writeMessage = new WriteMessage(byzantinePair, config.getPort(), consensusState.getInstance());
//        dcLogger.log("Broadcasting: " + writeMessage);
//        broadCastMessage(writeMessage);
//        return writePhase();
//    }
//
//    // Byzantines send the same AcceptMessage
//
//    public boolean writePhase() {
//        dcLogger.log("Waiting for write quorum of size: " + config.getByzantineQuorum() + "...");
//        ValueTimestampPair writeValue = this.consensusState.waitForWriteQuorum(config.getByzantineQuorum());
//        if (writeValue == null) {
//            // Abort
//            dcLogger.log("ABORTED (WRITE)");
//            this.consensusState.nextEpoch();
//            return false;
//        }
//        dcLogger.log("Quorum of WRITE reached");
//        ValueTimestampPair writeValts = new ValueTimestampPair(this.consensusState.getEpoch(), new ConsensusString("Byzantine"));
//        this.consensusState.setCurrent(writeValts);
//
//        // Broadcast ACCEPT and wait for quorum to DECIDE value
//        ValueTimestampPair byzantinePair = new ValueTimestampPair(this.consensusState.getEpoch(), new ConsensusString("Byzantine"));
//        AcceptMessage acceptMessage = new AcceptMessage(byzantinePair, config.getPort(), consensusState.getInstance());
//        dcLogger.log("Broadcasting: " + acceptMessage);
//        broadCastMessage(acceptMessage);
//
//        dcLogger.log("Waiting for accept quorum of size: " + config.getByzantineQuorum() + "...");
//        ValueTimestampPair accept = this.consensusState.waitForAcceptQuorum(config.getByzantineQuorum());
//        if (accept == null) {
//            // Abort
//            dcLogger.log("ABORTED (ACCEPT)");
//            this.consensusState.nextEpoch();
//            return false;
//        }
//        dcLogger.log("Quorum of accept reached");
//
//        // DECIDE value
//        if (accept.getValue().isString()) {
//            ConsensusString consensusString = (ConsensusString) accept.getValue();
//            this.stringChain.appendString(consensusString.getValue());
//            if (isLeader()) {
//                ClientReplyMessage clientReplyMessage = new ClientReplyMessage(
//                        consensusString.getValue(),
//                        true,
//                        consensusState.getInstance(),
//                        MessageType.STRING_REPLY
//                );
//                broadCastToClients(clientReplyMessage);
//            }
//            this.consensusState.nextInstance();
//        }
//        return true;
//    }
}
