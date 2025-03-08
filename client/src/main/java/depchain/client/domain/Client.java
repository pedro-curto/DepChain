package depchain.client.domain;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.domain.Entity;
import depchain.common.messaging.AppendMessage;
import depchain.common.messaging.Message;
import depchain.common.Security;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private final int port;
	private final int leaderPort;
    private final List<Entity> members;
    private final KeyPair clientKeys;
    private PerfectLink perfectLink;
    private BlockingQueue<Message> messageQueue;
    private final DCLogger dcLogger;

    public Client(String clientName, int port, List<Entity> members) {
		this.port = port;
        this.members = members;
        this.leaderPort = members.get(0).getPort();
        this.dcLogger = new DCLogger(Client.class);
        this.clientKeys = Security.getMemberKeyPair(clientName);
    }

    public void start() throws Exception {
        // init socket, messageQueue and the perfectLink abstraction
        DatagramSocket socket = new DatagramSocket(port);;
        messageQueue = new LinkedBlockingQueue<>();
        // start sessions
        List<Entity> entities = new ArrayList<>(members);
        this.perfectLink = new PerfectLink(socket, messageQueue, this.clientKeys, entities);
        perfectLink.start();
        perfectLink.startSession(this.leaderPort);
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
            AppendMessage msg = new AppendMessage(content, this.port);
            String signature = Security.makeDS(msg.getDataToSign(), clientKeys.getPrivate());
            msg.setSignature(signature);
            perfectLink.sendMessage(msg, leaderPort);
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
