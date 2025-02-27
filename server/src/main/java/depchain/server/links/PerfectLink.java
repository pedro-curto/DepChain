package depchain.server.links;

import java.net.*;
import java.io.IOException;
import java.util.UUID;

public class PerfectLink {

    private DatagramSocket socket;

    public PerfectLink(DatagramSocket socket) {
        this.socket = socket;
    }
    
    public void startListening() {
        byte[] buffer = new byte[1024];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                System.out.println("Waiting for message...");
                socket.receive(packet);
                System.out.println("Received message: " + new String(packet.getData(), 0, packet.getLength()));
                UUID msgId = extractUUIDFromMessage(packet);
                System.out.println("Sending ack for message " + msgId);
                sendAck(packet.getAddress(), packet.getPort(), msgId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private UUID extractUUIDFromMessage(DatagramPacket packet) {
        String message = new String(packet.getData(), 0, packet.getLength());
        String[] parts = message.split("\\|\\|", 2);
        System.out.println("received message: " + parts[1]);
        System.out.println("received message id: " + parts[0]);   
        return UUID.fromString(parts[0]);
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
}