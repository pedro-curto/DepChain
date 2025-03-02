package depchain.client.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.UUID;

public class PerfectLink {

    private DatagramSocket socket;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    private final Map<String, ScheduledFuture<?>> msgTasks = new ConcurrentHashMap<>();
    private final BlockingQueue<String> messageQueue;

    public PerfectLink(DatagramSocket socket, BlockingQueue<String> messageQueue) {
        this.socket = socket;
        this.messageQueue = messageQueue;
        startAckListener();
    }

    public void sendMessage(DatagramPacket packet, String msgId) {
        System.out.println("Sending message with id: " + msgId);
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        System.out.println("Scheduled message with id: " + msgId);
        msgTasks.put(msgId, task);
    }

    private void startAckListener() {
        System.out.println("Starting ack listener thread");
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                try {
                    System.out.println("Waiting for ack...");
                    socket.receive(ackPacket);

                    String ackId = extractUUIDFromAck(ackPacket).toString();
                    System.out.println("received ack id: " + ackId);
                    ScheduledFuture<?> task = msgTasks.remove(ackId);
                    System.out.println("Removed task for message " + ackId);
                    System.out.println("Remaining tasks: " + msgTasks.keySet());
                    if (task != null) {
                        task.cancel(true);
                        System.out.println("Message " + ackId + " acknowledged");

                        deliverMessage(ackId);
                        System.out.println("Delivered message " + ackId);

                    } else {
                        System.out.println("Received an unknown ack: " + ackId);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private UUID extractUUIDFromAck(DatagramPacket ackPacket) {
        String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
        System.out.println("Extracted ack: " + ack);
        if (ack.startsWith("ACK:")) {
            ack = ack.substring(4);
            return UUID.fromString(ack);
        }
        throw new IllegalArgumentException("Invalid ack: " + ack);
    }

    private void deliverMessage(String message) {
        System.out.println("Delivering message: " + message);
        messageQueue.offer(message);
    }
}