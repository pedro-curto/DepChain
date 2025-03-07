package depchain.member.links;

import com.google.gson.Gson;
import depchain.common.messaging.Message;
import depchain.member.membership.Member;
import depchain.common.messaging.MessageType;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.*;

public class PerfectLink {

    private final DatagramSocket socket;
    private final BlockingQueue<Message> messageQueue;
    private long receiveCounter;
    private long sendCounter;
    private final Member myself;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    private final Map<Long, ScheduledFuture<?>> msgTasks = new ConcurrentHashMap<>();
    private final Map<Long, Message> msgsToDeliver = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public PerfectLink(DatagramSocket socket, BlockingQueue<Message> messageQueue, Member myself) {
        this.socket = socket;
        this.messageQueue = messageQueue;
        this.myself = myself;
        this.sendCounter = 0;
        this.receiveCounter = 0;
    }

    public void start() {
        new Thread(this::startListening).start();
    }

    public void sendMessage(Message message, InetAddress address, int port) {
       System.out.println("[PerfectLink] Sending message with sequence number: " + this.sendCounter);
       message.setSequenceNumber(sendCounter);
       String json = gson.toJson(message);
       DatagramPacket packet = new DatagramPacket(
                json.getBytes(),
                json.length(),
                address,
                port);
       scheduleMessage(packet, sendCounter);
       msgsToDeliver.put(sendCounter, message);
       sendCounter++;
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

            if (type == MessageType.ACK) {
                handleAck(sequenceNumber);
            } else {
                handleContentMessage(sequenceNumber, message, packet.getAddress(), packet.getPort());
            }
        }
    }

    public void handleAck(long sequenceNumber) {
        System.out.println("[PerfectLink] Received ack for message with sequence number " + sequenceNumber);
        if (sequenceNumber < this.sendCounter) {
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

    public void handleContentMessage(long sequenceNumber, Message message, InetAddress address, int port) {
        System.out.println("[PerfectLink] Received content message with sequence number " + sequenceNumber);
        if (sequenceNumber == this.receiveCounter) {
            System.out.println("[PerfectLink] Received new content message");
            deliverMessage(message);
            this.receiveCounter++;
            sendAck(address, port, sequenceNumber);
            return;
        }
        if (sequenceNumber < this.receiveCounter) {
            System.out.println("[PerfectLink] Already received message");
            sendAck(address, port, sequenceNumber);
            return;
        }
        System.out.println("[PerfectLink] Message with invalid sequence number");
    }

    private void sendAck(InetAddress address, int port, long seqNumber) {
        Message ack = new Message(seqNumber, myself.getMemberName(), null, null, MessageType.ACK);
        byte[] ackData = gson.toJson(ack).getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        try {
            System.out.println("Sending ack: " + ack + " to " + address + ":" + port);
            socket.send(ackPacket);
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