package depchain.member.byzantine;

import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.*;
import depchain.member.domain.Member;

import java.util.List;

public class SpamByzantine extends Member {

    private static final int SPAM_MESSAGE_NUMBER = 10;

    public SpamByzantine(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
        super(memberName, members, clients, port, address, debug);
        System.out.println("SpamByzantine started at port " + port);
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
