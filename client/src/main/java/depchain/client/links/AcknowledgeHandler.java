package depchain.client.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.Map;

public class AcknowledgeHandler {

    private final DatagramSocket socket;
    private final boolean debug;
    private final Map<String, ScheduledFuture<?>> msgTasks;

    public AcknowledgeHandler(DatagramSocket socket, Map<String, ScheduledFuture<?>> msgTasks, boolean debug) {
        this.socket = socket;
        this.msgTasks = msgTasks;
        this.debug = debug;
    }

    public void start() {
        if (debug) System.out.println("[AcknowledgeHandler] Starting ack listener thread");
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                try {
                    if (debug) System.out.println("[AcknowledgeHandler] Waiting for ack...");
                    socket.receive(ackPacket);
                    String ackId = extractUUIDFromAck(ackPacket).toString();
                    if (debug) System.out.println("[AcknowledgeHandler] Received ack id: " + ackId);
                    ScheduledFuture<?> future = msgTasks.remove(ackId);
                    if (debug) System.out.println("[AcknowledgeHandler] Removed task for message " + ackId);
                    if (future != null) {
                        future.cancel(true);
                        if (debug) System.out.println("[AcknowledgeHandler] Message " + ackId + " acknowledged");
                    } else {
                        if (debug) System.out.println("[AcknowledgeHandler] Received an unknown ack: " + ackId);
                    }
                    // TODO -> check: should we send an ack back?
                    //sendAck(ackPacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private UUID extractUUIDFromAck(DatagramPacket ackPacket) {
        String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
        //if (debug) System.out.println("Extracted ack: " + ack);
        if (ack.startsWith("ACK:")) {
            ack = ack.substring(4);
            return UUID.fromString(ack);
        }
        throw new IllegalArgumentException("Invalid ack: " + ack);
    }

    private void sendAck(DatagramPacket ackPacket) throws IOException {
        String ackResponse = "ACK:" + new String(ackPacket.getData(), 0, ackPacket.getLength());
        DatagramPacket responsePacket = new DatagramPacket(
                ackResponse.getBytes(),
                ackResponse.length(),
                ackPacket.getAddress(),
                ackPacket.getPort()
        );
        socket.send(responsePacket);
        if (debug) System.out.println("Sent ack response: " + ackResponse);
    }
}