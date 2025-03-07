package depchain.common;

import com.google.gson.Gson;
import depchain.common.messaging.KeyExchangeMessage;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;
import depchain.common.session.Session;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;

public class PerfectLink {

    private final DatagramSocket socket;
    private final BlockingQueue<Message> messageQueue;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    private final Map<Long, ScheduledFuture<?>> msgTasks = new ConcurrentHashMap<>();
    private final Map<Long, Message> msgsToDeliver = new ConcurrentHashMap<>();
    private final Map<Integer, Session> sessions = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final DCLogger dcLogger;

    public PerfectLink(DatagramSocket socket, BlockingQueue<Message> messageQueue) {
        this.socket = socket;
        this.messageQueue = messageQueue;
        this.dcLogger = new DCLogger(PerfectLink.class);
    }

    public void start() {
        new Thread(this::startListening).start();
    }

    public void startSession(String address, int port, KeyPair memberKey, PublicKey otherPub) {
        SecretKey sessionKey = Security.generateSecretKey();
        if (sessionKey == null) dcLogger.log("Failed to generate session key");
        Session newSession = new Session(sessionKey, port, address);
        sessions.put(port, newSession);
        // content = encrypt session key with otherPub || memberKey.pubkey.
        byte[] encryptKeyBytes = Security.encryptSymKeyWithAsymKey(sessionKey, otherPub);
        String encryptKey = Base64.getEncoder().encodeToString(encryptKeyBytes);
        String myPubKey = Base64.getEncoder().encodeToString(memberKey.getPublic().getEncoded());
        // mac
        String dataForSig = encryptKey + myPubKey;
        String signature = null;
        try {
            signature = Security.makeDS(dataForSig, memberKey.getPrivate());
        } catch (Exception e) {
           dcLogger.log("Error generating signature");
        }
        if (signature == null) {
            dcLogger.log("signature generation failed, aborting session");
            sessions.remove(port);
            return;
        }
        // send message and increase send counter
        KeyExchangeMessage message = new KeyExchangeMessage(newSession.getSendCounter(), myPubKey, encryptKey, signature);
        sendMessage(message, port);
        newSession.incrementSendCounter();
    }

    public void sendMessage(Message message, int port) {
       Session session = sessions.get(port);
       long sequenceNumber = session.getSendCounter();
       dcLogger.log("Sending message with sequence number: " + sequenceNumber);
       message.setSequenceNumber(sequenceNumber);
       String json = gson.toJson(message);
       try {
           DatagramPacket packet = new DatagramPacket(
                    json.getBytes(),
                    json.length(),
                    InetAddress.getByName(session.getAddress()),
                    session.getPort());
           scheduleMessage(packet, sequenceNumber);
       } catch (UnknownHostException e) {
           throw new RuntimeException(e);
       }
       msgsToDeliver.put(sequenceNumber, message);
       sequenceNumber++;
       session.setSendCounter(sequenceNumber);
    }

    private void scheduleMessage(DatagramPacket packet, long sequenceNumber) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        msgTasks.put(sequenceNumber, task);
        dcLogger.log("Scheduled message with sequenceNumber: " + sequenceNumber);
    }

    private void startListening() {
        byte[] buffer = new byte[1024];
        while (true) {
            dcLogger.log("Waiting for message...");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String received = new String(packet.getData(), 0, packet.getLength());
            Message message = gson.fromJson(received, Message.class);
            dcLogger.log("Received message: " + received);

            MessageType type = message.getType();
            long sequenceNumber = message.getSequenceNumber();
            dcLogger.log("Received Message with type " + type + " and sequence number " + sequenceNumber);

            if (type == MessageType.KEY_EXCHANGE) {
                handleSessionRequest(String.valueOf(packet.getAddress()), packet.getPort(), message);
                return;
            }

            Session session = sessions.get(packet.getPort());

            if (type == MessageType.ACK) {
                handleAck(sequenceNumber, session);
                return;
            }
            handleContentMessage(sequenceNumber, message, session);
        }
    }

    private void handleSessionRequest(String address, int port, KeyExchangeMessage message) {
        dcLogger.log("Received session request: " + address + ":" + port);
        if (message.getSequenceNumber() != 0) {
            dcLogger.log("Expected sequence number 0 but got " + message.getSequenceNumber());
        }
        String pubkey = message.getPublicKey();
        String encryptedSessionKey = message.getEncryptedSessionKey();
        String signature = message.getSignature();
    }

    private void handleAck(long sequenceNumber, Session session) {
        dcLogger.log("Received ack for message with sequence number " + sequenceNumber);
        if (sequenceNumber < session.getSendCounter()) {
            ScheduledFuture<?> task = msgTasks.remove(sequenceNumber);
            if (task != null) {
                task.cancel(true);
                dcLogger.log("[PerfectLink-AckListener] ACK received, cancelled retransmission for message " + sequenceNumber);

                Message message = msgsToDeliver.remove(sequenceNumber);
                if (message != null) {
                    deliverMessage(message);
                }
                return;
            }
            dcLogger.log("[PerfectLink-AckListener] Received a duplicate ACK: " + sequenceNumber);
        }
        dcLogger.log("[PerfectLink-AckListener] Received an unknown ACK: " + sequenceNumber);
    }

    private void handleContentMessage(long sequenceNumber, Message message, Session session) {
        dcLogger.log("Received content message with sequence number " + sequenceNumber);
        long counter = session.getReceiveCounter();
        if (sequenceNumber == counter) {
            dcLogger.log("Received new content message");
            deliverMessage(message);
            counter++;
            session.setReceiveCounter(counter);
            sendAck(session, sequenceNumber);
            return;
        }
        if (sequenceNumber < counter) {
            dcLogger.log("Already received message");
            sendAck(session, sequenceNumber);
            return;
        }
        dcLogger.log("Message with invalid sequence number");
    }

    private void sendAck(Session session, long seqNumber) {
        Message ack = new Message(seqNumber,null, null, null, MessageType.ACK);
        byte[] ackData = gson.toJson(ack).getBytes();
        try {
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, InetAddress.getByName(session.getAddress()), session.getPort());
            dcLogger.log("Sending ack: " + ack + " to " + session.getAddress() + ":" + session.getPort());
            socket.send(ackPacket);
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deliverMessage(Message message) {
        dcLogger.log("Delivering message: " + message);
        if (messageQueue.offer(message)) {
            dcLogger.log("Message delivered: " + message);
        }
        else {
            dcLogger.log("Message queue is full, unable to deliver message: " + message);
        }
    }
}