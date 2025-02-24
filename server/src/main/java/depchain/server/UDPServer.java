package depchain.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPServer {
    private static final int port = 5001;

    public static void main (String[] args) throws IOException {
        System.out.println("Server started and listening on port " + port);
        DatagramSocket sock = new DatagramSocket(port);
        byte[] recvData = new byte[1024];
        DatagramPacket recvPacket;
        while (true) {
            recvPacket = new DatagramPacket(recvData, recvData.length);
            sock.receive(recvPacket);
            String recvMsg = new String(recvPacket.getData(), 0, recvPacket.getLength());
            System.out.println("Received: " + recvMsg);
            InetAddress addr = recvPacket.getAddress();
            int port = recvPacket.getPort();
            System.out.println("From: " + addr + ":" + port);
        }
    }
}
