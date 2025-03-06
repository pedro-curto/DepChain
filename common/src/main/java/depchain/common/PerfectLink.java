package depchain.common;

import com.google.gson.Gson;
import depchain.common.session.Session;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.*;

public class PerfectLink {

    private final DatagramSocket socket;
    private final BlockingQueue<Message> messageQueue;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    private final Map<Long, ScheduledFuture<?>> msgTasks = new ConcurrentHashMap<>();
    private final Map<Long, Message> msgsToDeliver = new ConcurrentHashMap<>();
    private final Map<Integer, Session> sessions;
    private final Gson gson = new Gson();

    public PerfectLink(DatagramSocket socket, BlockingQueue<Message> messageQueue, Map<Integer, Session> sessions) {
        this.socket = socket;
        this.messageQueue = messageQueue;
        this.sessions = sessions;
    }

    public void start() {
        new Thread(this::startListening).start();
    }

    public void sendMessage(Message message, Session session) {
       long sequenceNumber = session.getSendCounter();
       System.out.println("[PerfectLink] Sending message with sequence number: " + sequenceNumber);
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

    public void scheduleMessage(DatagramPacket packet, long sequenceNumber) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        msgTasks.put(sequenceNumber, task);
        System.out.println("[PerfectLink] Scheduled message with sequenceNumber: " + sequenceNumber);
    }

    private void startListening() {
        byte[] buffer = new byte[1024];
        while (true) {
            System.out.println("[PerfectLink] Waiting for message...");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String received = new String(packet.getData(), 0, packet.getLength());
            Message message = gson.fromJson(received, Message.class);
            System.out.println("[PerfectLink] Received message: " + received);

            MessageType type = message.getType();
            long sequenceNumber = message.getSequenceNumber();
            System.out.println("[PerfectLink] Received Message with type " + type + " and sequence number " + sequenceNumber);

            Session session = sessions.get(packet.getPort());

            if (type == MessageType.ACK) {
                handleAck(sequenceNumber, session);
            } else {
                handleContentMessage(sequenceNumber, message, session);
            }
        }
    }

    public void handleAck(long sequenceNumber, Session session) {
        System.out.println("[PerfectLink] Received ack for message with sequence number " + sequenceNumber);
        if (sequenceNumber < session.getSendCounter()) {
            ScheduledFuture<?> task = msgTasks.remove(sequenceNumber);
            if (task != null) {
                task.cancel(true);
                System.out.println("[PerfectLink-AckListener] ACK received, cancelled retransmission for message " + sequenceNumber);

                Message message = msgsToDeliver.remove(sequenceNumber);
                if (message != null) {
                    deliverMessage(message);
                }
                return;
            }
            System.out.println("[PerfectLink-AckListener] Received a duplicate ACK: " + sequenceNumber);
        }
        System.out.println("[PerfectLink-AckListener] Received an unknown ACK: " + sequenceNumber);
    }

    public void handleContentMessage(long sequenceNumber, Message message, Session session) {
        System.out.println("[PerfectLink] Received content message with sequence number " + sequenceNumber);
        long counter = session.getReceiveCounter();
        if (sequenceNumber == counter) {
            System.out.println("[PerfectLink] Received new content message");
            deliverMessage(message);
            counter++;
            session.setReceiveCounter(counter);
            sendAck(session, sequenceNumber);
            return;
        }
        if (sequenceNumber < counter) {
            System.out.println("[PerfectLink] Already received message");
            sendAck(session, sequenceNumber);
            return;
        }
        System.out.println("[PerfectLink] Message with invalid sequence number");
    }

    private void sendAck(Session session, long seqNumber) {
        Message ack = new Message(seqNumber,null, null, null, MessageType.ACK);
        byte[] ackData = gson.toJson(ack).getBytes();
        try {
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, InetAddress.getByName(session.getAddress()), session.getPort());
            System.out.println("Sending ack: " + ack + " to " + session.getAddress() + ":" + session.getPort());
            socket.send(ackPacket);
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deliverMessage(Message message) {
        System.out.println("Delivering message: " + message);
        if (messageQueue.offer(message)) {
            System.out.println("Message delivered: " + message);
        }
        else {
            System.out.println("Message queue is full, unable to deliver message: " + message);
        }
    }
}