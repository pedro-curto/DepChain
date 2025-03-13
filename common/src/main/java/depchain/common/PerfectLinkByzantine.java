package depchain.common;

import depchain.common.domain.Entity;
import depchain.common.messaging.Message;
import depchain.common.session.Session;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class PerfectLinkByzantine extends PerfectLink {

    private static final float LOST_CHANCES = 0.3f;

    private Random random = new Random();

    public PerfectLinkByzantine(DatagramSocket socket, BlockingQueue<Message> messageQueue, KeyPair personalKeys, List<Entity> entities, boolean debug) {
        super(socket, messageQueue, personalKeys, entities, debug);
    }

    private boolean isLostMessage() {
        return random.nextFloat() < LOST_CHANCES;
    }

    @Override
    public void handleContentMessage(long sequenceNumber, Message message, Session session) {
        if (!isLostMessage()) {
            super.handleContentMessage(sequenceNumber, message, session);
        }
        dcLogger.log("MESSAGE WAS LOST... " + message);
    }

    @Override
    public void handleAck(Message ackMessage, Session session) {
        if (!isLostMessage()) {
            super.handleAck(ackMessage, session);
        }
        dcLogger.log("ACK WAS LOST... " + ackMessage);
    }
}
