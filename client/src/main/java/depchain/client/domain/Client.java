package depchain.client.domain;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.domain.Entity;
import depchain.common.messaging.AppendMessage;
import depchain.common.messaging.ClientReplyMessage;
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
    private final boolean debug;

    public Client(String clientName, int port, List<Entity> members, boolean debug) {
        this.debug = debug;
		this.port = port;
        this.members = members;
        this.leaderPort = members.get(0).getPort();
        this.dcLogger = new DCLogger(Client.class, debug);
        this.clientKeys = Security.getMemberKeyPair(clientName);
    }

    public void start() throws Exception {
        // init socket, messageQueue and the perfectLink abstraction
        DatagramSocket socket = new DatagramSocket(port);;
        messageQueue = new LinkedBlockingQueue<>();
        // start sessions
        List<Entity> entities = new ArrayList<>(members);
        this.perfectLink = new PerfectLink(socket, messageQueue, this.clientKeys, entities, this.debug);
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
        System.out.print("> ");
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
            if (message instanceof ClientReplyMessage) {
                ClientReplyMessage appendMessage = (ClientReplyMessage) message;
                boolean success = appendMessage.getSuccess();
                String outcome;
                int consensusInstance = appendMessage.getInstanceOfDecision();
                if (success) {
                    outcome = "successfully appended";
                } else {
                    outcome = "not appended";
                }
                System.out.println("String " + appendMessage.getValue() +
                        " was " + outcome + " to the blockchain at timestamp " + consensusInstance);
                System.out.print("> ");
            } else {
                System.out.println("[SERVER GOT]: " + message);
            }
        }
    }
}
