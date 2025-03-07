package depchain.client.domain;

import com.google.gson.Gson;
import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.Leader;
import depchain.common.messaging.AckMessage;
import depchain.common.messaging.AppendMessage;
import depchain.common.messaging.Message;
import depchain.common.Security;
import depchain.common.session.Session;

import javax.crypto.SecretKey;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private final int port;
    private final String myName;
    private final Leader leader;
    private PerfectLink perfectLink;
    private BlockingQueue<Message> messageQueue;
    private DCLogger dcLogger;

    public Client(String clientName, int port, Leader leader) {
        this.myName = clientName;
        this.port = port;
        this.leader = leader;
        this.dcLogger = new DCLogger(Client.class);
    }

    public void start() throws Exception {
        // init socket, messageQueue and the perfectLink abstraction
        DatagramSocket socket = new DatagramSocket(port);
        KeyPair myKeyPair = Security.getMemberKeyPair(myName);
        messageQueue = new LinkedBlockingQueue<>();
        perfectLink = new PerfectLink(socket, messageQueue, myKeyPair);
        // handshake with leader to deliver the session key
        PublicKey leaderPubKey = Security.getMemberPublicKey(leader.getName());
        perfectLink.start();
        perfectLink.startSession(leader.getAddress(), leader.getPort(), myKeyPair, leaderPubKey);
        // start a thread to deliver incoming messages
        Thread messageDeliveringThread = new Thread(() -> deliverMessage(messageQueue));
        messageDeliveringThread.start();
        // start processing user input
        processUserInput();
    }

    private void processUserInput() throws Exception {
        Scanner input = new Scanner(System.in);
        while (true) {
            String content = input.nextLine();
            if (content.equals("QUIT")) {
                input.close();
                System.exit(0);
            }
            AppendMessage msg = new AppendMessage(content);
            perfectLink.sendMessage(msg, leader.getPort());
            dcLogger.log("Sent message: " + msg);
        }
    }

    private static void deliverMessage(BlockingQueue<Message> messageQueue) {
        while (true) {
            Message message;
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            System.out.println("[SERVER GOT]: " + message);
        }
    }
}
