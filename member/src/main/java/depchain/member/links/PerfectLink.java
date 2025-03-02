package depchain.member.links;

import com.google.gson.Gson;

import depchain.common.Message;
import depchain.common.SignatureUtils;
import depchain.member.membership.Member;
import depchain.member.membership.MemberData;
import depchain.member.messaging.MessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class PerfectLink {

    private DatagramSocket socket;
    private MessageHandler messageHandler;
    private final BlockingQueue<String> messageQueue;
    private final Map<String, String> receivedMessages = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final Member myself;
    // tasks to resend a message until an ack is received
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    private final Map<String, ScheduledFuture<?>> msgTasks = new ConcurrentHashMap<>();

    public PerfectLink(DatagramSocket socket, BlockingQueue<String> messageQueue, Member myself) {
        this.socket = socket;
        this.messageQueue = messageQueue;
        this.myself = myself;
    }

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public void sendMessage(DatagramPacket packet, String msgId) {
        System.out.println("[PerfectLink] Sending message with id: " + msgId);
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        System.out.println("[PerfectLink] Scheduled message with id: " + msgId);
        msgTasks.put(msgId, task);
    }

    public void start() {
        new Thread(this::startListening).start();
        //new Thread(this::startAckListener).start();
    }

    private void startListening() {
        byte[] buffer = new byte[1024];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                System.out.println("Waiting for message...");
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received message: " + received);

                // gets message and checks if we already received it
                Message message = gson.fromJson(received, Message.class);
                UUID msgId = UUID.fromString(message.getMsgId());
                System.out.println("Checking if message is already received...");
                if (receivedMessages.containsKey(msgId.toString())) {
                    System.out.println("Message already received, sending ack...");
                    sendAck(packet.getAddress(), packet.getPort(), msgId);
                    continue;
                }
                System.out.println("Storing message in map...");
                receivedMessages.put(msgId.toString(), received);

                // passes it to the message handler
                System.out.println("Handling message...");
                if (messageHandler != null) {
                    messageHandler.handleMessage(received, packet.getAddress(), packet.getPort());
                }
                // sends ack
                //System.out.println("Sending ack for message " + msgId);
                //sendAck(packet.getAddress(), packet.getPort(), msgId);
                // delivers message
                System.out.println("Delivering message...");
                deliverMessage(message.getMsgContent());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendAck(InetAddress address, int port, UUID msgId) {
        // message with msgType clientAck and msgId of myself
        Message ack = new Message(msgId.toString(), myself.getMemberName(), null, null, "clientAck");
        byte[] ackData = gson.toJson(ack).getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        try {
            System.out.println("Sending ack: " + ack + " to " + address + ":" + port);
            socket.send(ackPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deliverMessage(String message) {
        System.out.println("Delivering message: " + message);
        if (messageQueue.offer(message) == false) {
            System.out.println("Message queue is full, unable to deliver message: " + message);
            return;
        }
        System.out.println("Message delivered: " + message);
    }

    public void sendAckToClient(DatagramPacket ackPacket, String msgId) {
        try {
            System.out.println("[PerfectLink] Sending ack to client with id: " + msgId);
            socket.send(ackPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO -> acho que o ackListener é inútil
    private void startAckListener() {
        byte[] buffer = new byte[1024];
        while (true) {
            DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
            try {
                System.out.println("[PerfectLink-AckListener] Waiting for ACK...");
                socket.receive(ackPacket);
                String ackData = new String(ackPacket.getData(), 0, ackPacket.getLength());
                System.out.println("[PerfectLink-AckListener] Received ACK: " + ackData);
                if (ackData.startsWith("ACK:")) {
                    String ackId = ackData.substring(4);
                    ScheduledFuture<?> task = msgTasks.remove(ackId);
                    if (task != null) {
                        task.cancel(true);
                        System.out.println("[PerfectLink-AckListener] ACK received, cancelled retransmission for message " + ackId);
                    } else {
                        System.out.println("[PerfectLink-AckListener] Received unknown ACK: " + ackId);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}