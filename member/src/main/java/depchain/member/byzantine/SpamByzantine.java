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

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class SpamByzantine extends Member {

    private static final int SPAM_MESSAGE_NUMBER = 10;

    public SpamByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, BlockchainState bcState, BlockingQueue<Message> messageQueue, BlockingQueue<AppendMessage> appendQueue) {
        super(config, dcLogger, pf, cState, bcState, messageQueue, appendQueue);
    }

    @Override
    public void sendToLeader(Message message) {
        dcLogger.log("Spamming " + SPAM_MESSAGE_NUMBER + " " + message.getType() + "s...");
        for (int i = 0; i < SPAM_MESSAGE_NUMBER; i++) {
            super.sendToLeader(message);
        }
    }

    @Override
    public void broadCastMessage(Message message) {
        dcLogger.log("Spamming " + SPAM_MESSAGE_NUMBER + " " + message.getType() + "s...");
        for (int i = 0; i < SPAM_MESSAGE_NUMBER; i++) {
            super.broadCastMessage(message);
        }
    }
}
