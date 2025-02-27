package depchain.server;

import java.io.IOException;
import java.net.DatagramSocket;
import depchain.server.links.PerfectLink;

public class UDPServer {
    private static final int port = 5001;

    public static void main (String[] args) throws IOException {

        System.out.println("Server started and listening on port " + port);
        DatagramSocket serverSocket = new DatagramSocket(port);
        PerfectLink pf = new PerfectLink(serverSocket);
        pf.startListening();
    }
}
