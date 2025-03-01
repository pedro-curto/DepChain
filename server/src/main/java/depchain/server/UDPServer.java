package depchain.server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import depchain.server.links.PerfectLink;

public class UDPServer {
    private static final int port = 5001;

    public static void main (String[] args) throws IOException {

        System.out.println("Server started and listening on port " + port);
        DatagramSocket serverSocket = new DatagramSocket(port);
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        PerfectLink pf = new PerfectLink(serverSocket, messageQueue);
        pf.start();

        while (true) {
            String message = null;
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            System.out.println("[MESSAGE]: " + message);
        }
    }
}
