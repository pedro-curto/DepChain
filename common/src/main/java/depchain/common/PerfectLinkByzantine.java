package depchain.common;

import com.google.gson.Gson;
import depchain.common.domain.Entity;
import depchain.common.messaging.Message;
import depchain.common.session.Session;
import depchain.common.session.SessionTaskKey;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.KeyPair;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PerfectLinkByzantine extends PerfectLink {

    private static final float LOST_CHANCES = 0f;

    private static final float DUPLICATE_CHANCES = 0.5f;

    private static final float DELAY_CHANCES = 0f;

    private static final float CORRUPTED_CHANCES = 0;

    private static final int DELAY_TIME = 1; // seconds

    private Random random = new Random();

    public PerfectLinkByzantine(DatagramSocket socket, BlockingQueue<Message> messageQueue, KeyPair personalKeys, List<Entity> entities, boolean debug) {
        super(socket, messageQueue, personalKeys, entities, debug);
    }

    private boolean isLostMessage() {
        return random.nextFloat() < LOST_CHANCES;
    }

    private boolean isDuplicateMessage() {
        return random.nextFloat() < DUPLICATE_CHANCES;
    }

    private boolean isDelayedMessage() {
        return random.nextFloat() < DELAY_CHANCES;
    }

    private boolean isCorruptedMessage() {
        return random.nextFloat() < CORRUPTED_CHANCES;
    }

    @Override
    public void handleContentMessage(long sequenceNumber, Message message, Session session) {
        if (isLostMessage()) {
            // Message lost
            dcLogger.log("MESSAGE WAS LOST... : " + message);
            return;
        }
        else if (isDuplicateMessage()) {
            // Duplicated message
            dcLogger.log("MESSAGE WAS DUPLICATED... : " + message);
            super.handleContentMessage(sequenceNumber, message, session);
            super.handleContentMessage(sequenceNumber, message, session);
            return;
        }
        super.handleContentMessage(sequenceNumber, message, session);

    }

    @Override
    public void handleAck(Message ackMessage, Session session) {
        if (isLostMessage()) {
            // Message lost
            dcLogger.log("MESSAGE WAS LOST... : " + ackMessage);
            return;
        }
        else if (isDuplicateMessage()) {
            // Duplicated message
            dcLogger.log("MESSAGE WAS DUPLICATED... : " + ackMessage);
            super.handleAck(ackMessage, session);
            super.handleAck(ackMessage, session);
            return;
        }
        super.handleAck(ackMessage, session);
    }

    // Delaying messages
    @Override
    public void scheduleMessage(DatagramPacket packet, SessionTaskKey key) {
        if (isDelayedMessage()) {
            dcLogger.log("MESSAGE WAS DELAYED... ");
            scheduleMessageDelay(packet,key, DELAY_TIME);
        } else {
            super.scheduleMessage(packet, key);
        }
    }

    public void scheduleMessageDelay(DatagramPacket packet, SessionTaskKey key, int delay) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, delay, 2, TimeUnit.SECONDS);
        msgTasks.put(key, task);
    }

    // Corrupting Messages
    @Override
    public String convertToJson(Message message) {
        String json = new Gson().toJson(message);

        if (isCorruptedMessage()) {
            dcLogger.log("MESSAGE WAS CORRUPTED... : " + message);
            return corruptData(json);
        }
        return super.convertToJson(message);
    }

    private char getRandomChar() {
        // For getting a random ASCII char
        return (char) (32 + random.nextInt(95));
    }

    private String corruptData(String json) {
        Random random = new Random();
        StringBuilder corrupted = new StringBuilder(json);

        // Determine corruption type and number of corruptions
        // TODO change to 4 to use also bitflip
        int corruptionType = random.nextInt(3); // 4 for different corruption types
        int corruptionCount = 1 + random.nextInt(3); // 1-3 corruptions per message

        for (int i = 0; i < corruptionCount; i++) {
            int position = random.nextInt(corrupted.length());

            switch (corruptionType) {
                case 0: // Replace character
                    if (corrupted.length() > 0) {
                        corrupted.setCharAt(position, getRandomChar());
                    }
                    break;

                case 1: // Delete character
                    if (corrupted.length() > 0) {
                        corrupted.deleteCharAt(position);
                    }
                    break;

                case 2: // Insert character
                    corrupted.insert(position, getRandomChar());
                    break;

                case 3: // Bit flip
                    if (corrupted.length() > 0) {
                        char originalChar = corrupted.charAt(position);
                        int bitPosition = random.nextInt(8); // 8 bits in a byte
                        char corruptedChar = (char) (originalChar ^ (1 << bitPosition)); // XOR to flip bit
                        corrupted.setCharAt(position, corruptedChar);
                    }
                    break;
            }
        }

        return corrupted.toString();
    }
}
