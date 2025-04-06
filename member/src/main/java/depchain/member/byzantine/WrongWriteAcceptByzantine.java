package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.domain.*;
import depchain.common.messaging.*;
import depchain.common.messaging.consensus.AcceptMessage;
import depchain.common.messaging.consensus.StateMessage;
import depchain.common.messaging.consensus.WriteMessage;
import depchain.common.messaging.library.AppendMessage;
import depchain.member.domain.Config;
import depchain.member.domain.ConsensusHandler;
import depchain.member.domain.ConsensusLeaderState;
import depchain.member.domain.Member;
import depchain.member.state.StringChain;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class WrongWriteAcceptByzantine extends Member {

    ConsensusState consensusState;
    // Sends Write and Accept messages with the same byzantine value every time
    public WrongWriteAcceptByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, StringChain bcState, BlockingQueue<Message> messageQueue) {
        super(config, dcLogger, pf, cState, bcState, messageQueue);
        this.consensusHandler = new ByzantineConsensusHandler(this, dcLogger, cState);
        consensusState = super.consensusHandler.getConsensusState();
    }
}
