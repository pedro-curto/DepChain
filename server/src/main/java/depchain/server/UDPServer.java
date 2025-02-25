package depchain.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static depchain.server.utils.Links.receiveMessageWithLoss;

public class UDPServer {
    private static final int port = 5001;

    public static void main (String[] args) throws IOException {

        System.out.println("Server started and listening on port " + port);
        DatagramSocket sock = new DatagramSocket(port);
        byte[] recvData = new byte[1024];
        DatagramPacket recvPacket;

        while (true) {
            recvPacket = receiveMessageWithLoss(sock, recvData, recvData.length, true);

            if (recvPacket == null) {
                // Message lost
                System.out.println("MESSAGE LOST...");
            } else {
                // Message received
                String recvMsg = new String(recvPacket.getData(), 0, recvPacket.getLength());
                System.out.println("Received: " + recvMsg);
                System.out.println("From: " + recvPacket.getAddress() + ":" + recvPacket.getPort());
            }
        }
    }
}
