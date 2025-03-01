package depchain.server.links;

import java.net.*;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class PerfectLink {

    private DatagramSocket socket;
    private final BlockingQueue<String> messageQueue;
    private final Map<String, String> messageMap;

    public PerfectLink(DatagramSocket socket, BlockingQueue<String> messageQueue) {
        this.socket = socket;
        this.messageMap = new ConcurrentHashMap<>();
        this.messageQueue = messageQueue;
    }

    public void start() {
        Thread messageListenerThread = new Thread(this::startListening);
        messageListenerThread.start();
    }

    private void startListening() {
        byte[] buffer = new byte[1024];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                System.out.println("Waiting for message...");
                socket.receive(packet);

                System.out.println("Received message: " + new String(packet.getData(), 0, packet.getLength()));

                String[] parts = new String[2];
                UUID msgId = extractUUIDFromMessage(packet, parts);
                String message = parts[1];

                System.out.println("Checking if message is already received...");
                if (messageMap.containsKey(msgId.toString())) {
                    System.out.println("Message already received, sending ack...");
                    sendAck(packet.getAddress(), packet.getPort(), msgId);
                    continue;
                }

                System.out.println("Storing message in map...");
                messageMap.put(msgId.toString(), message);

                System.out.println("Sending ack for message " + msgId);
                sendAck(packet.getAddress(), packet.getPort(), msgId);

                System.out.println("Delivering message...");
                deliverMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAck(InetAddress address, int port, UUID msgId) {
        String ackMessage = "ACK:" + msgId.toString();
        byte[] ackData = ackMessage.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        try {
            System.out.println("Sending ack: " + ackMessage + " to " + address + ":" + port);
            socket.send(ackPacket);
        } catch (IOException e) {
            e.printStackTrace();
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
    
    private UUID extractUUIDFromMessage(DatagramPacket packet, String[] parts) {
        String message = new String(packet.getData(), 0, packet.getLength());
        String[] splitParts = message.split("\\|\\|", 2);
        parts[0] = splitParts[0];
        parts[1] = splitParts[1];
        System.out.println("received message: " + parts[1]);
        System.out.println("received message id: " + parts[0]);   
        return UUID.fromString(parts[0]);
    }
}