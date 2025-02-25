package depchain.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static depchain.server.utils.Links.receiveMessages;

public class UDPServer {
    private static final int port = 5001;

    public static void main (String[] args) throws IOException {

        System.out.println("Server started and listening on port " + port);
        DatagramSocket sock = new DatagramSocket(port);

        receiveMessages(sock, true);
    }
}
