package depchain.client.domain;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.common.messaging.AppendMessage;
import depchain.common.messaging.Message;
import depchain.common.Security;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private static String LEADER_FILE = "membership/leader.txt";
    private final int port;
    private final String myName;
    private Entity leader;
    private PerfectLink perfectLink;
    private BlockingQueue<Message> messageQueue;
    private DCLogger dcLogger;

    public Client(String clientName, int port) {
        this.myName = clientName;
        this.port = port;
        this.dcLogger = new DCLogger(Client.class);
    }

    public void start() throws Exception {
        // loads leader
        leader = CommonUtils.leaderLoader(LEADER_FILE);
        // init socket, messageQueue and the perfectLink abstraction
        DatagramSocket socket = new DatagramSocket(port);
        KeyPair myKeyPair = Security.getMemberKeyPair(myName);
        messageQueue = new LinkedBlockingQueue<>();
        perfectLink = new PerfectLink(socket, messageQueue, myKeyPair);
        // handshake with leader to deliver the session key
        PublicKey leaderPubKey = Security.getMemberPublicKey(leader.getEntityName());
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
