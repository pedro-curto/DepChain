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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private long nonce = 0;
    private String baseDir = System.getProperty("user.dir");
    private String clientName;
    private final int port;
	private final int leaderPort;
    private final List<Entity> members;
    // to deal with responses to APPEND requests
    private final int faultyProcesses;
    protected final int byzantineQuorum;
    // {string: state with answers and if it was decided}
    private final Map<String, AppendState> memberResponses;

    private KeyPair clientKeys;
    private PerfectLink perfectLink;
    private BlockingQueue<Message> messageQueue;
    private final DCLogger dcLogger;
    private final boolean debug;
    private volatile boolean running = true;
    private final boolean testEnvironment;

    public Client(String clientName, int port, List<Entity> members, boolean debug, boolean testEnvironment) {
        this.clientName = clientName;
        this.debug = false;
		this.port = port;
        this.members = members;
        this.faultyProcesses = Math.floorDiv(members.size() - 1, 3);
        this.byzantineQuorum = members.size() - faultyProcesses;
        this.memberResponses = new HashMap<>();
        // TODO hardcoded leader
        this.leaderPort = members.get(0).getPort();
        this.dcLogger = new DCLogger(Client.class, debug);
        this.testEnvironment = testEnvironment;
    }

    public void start() throws Exception {
        this.clientKeys = Security.getMemberKeyPair(baseDir, clientName);
        if (clientKeys == null) {
            dcLogger.log("Keys not loaded successfully.");
        }
        // init socket, messageQueue and the perfectLink abstraction
        DatagramSocket socket = new DatagramSocket(port);;
        messageQueue = new LinkedBlockingQueue<>();
        // start sessions
        List<Entity> entities = new ArrayList<>(members);
        this.perfectLink = new PerfectLink(socket, messageQueue, this.clientKeys, entities, false);
        perfectLink.start();
        // starts session with every member
        for (Entity member : members) {
            perfectLink.startSession(member.getPort());
        }
        //perfectLink.startSession(this.leaderPort);
        // start a thread to deliver incoming messages
        Thread messageDeliveringThread = new Thread(() -> deliverMessage(messageQueue));
        messageDeliveringThread.start();
        // start processing user input
        if (!testEnvironment) {
            processUserInput();
        }
    }

    public void stop() {
        perfectLink.stop();
        running = false;
    }

    private void processUserInput() throws Exception {
        Scanner input = new Scanner(System.in);
        System.out.print("> ");
        while (running) {
            String content = input.nextLine();
            if (content.equals("QUIT")) {
                input.close();
                System.exit(0);
            }
            sendAppend(content);
        }
    }

    public void sendAppend(String content) {
        AppendMessage msg = new AppendMessage(content, this.port, nonce);
        String signature = Security.makeDS(msg.getDataToSign(), clientKeys.getPrivate());
        msg.setSignature(signature);
        perfectLink.sendMessage(msg, leaderPort);
        dcLogger.log("Sent message: " + msg);
        // increment nonce after sending
        nonce++;
    }

    private void deliverMessage(BlockingQueue<Message> messageQueue) {
        while (running) {
            Message message;
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            if (message instanceof ClientReplyMessage) {
                ClientReplyMessage appendMessage = (ClientReplyMessage) message;
                String value = appendMessage.getValue();
                // if we didn't get an answer for the string we received yet create it in the map
                if (!memberResponses.containsKey(value)) {
                    memberResponses.put(value, new AppendState(this.faultyProcesses, this.byzantineQuorum));
                }
                AppendState appendState = memberResponses.get(value);
                // if we already decided we're happy, move on
                if (appendState.getAppended()) {
                    continue;
                }
                // increment counters and check
                int totalAnswersCounter = appendState.getTotalAnswersCounter();
                totalAnswersCounter++;
                System.out.println("Received " + totalAnswersCounter + " answers for value " + value);

                Map<String, Integer> equalAnswersCounter = appendState.getEqualAnswersCounter();
                int equalCount = equalAnswersCounter.getOrDefault(value, 0);
                if (appendMessage.getSuccess()) {
                    // increases the counter of equal answers
                    equalCount++;
                    equalAnswersCounter.put(value, equalCount);
                    System.out.println("Answers map: " + equalAnswersCounter);
                }
                // if the number of equal answers is greater than the quorum, print the outcome
                if (equalCount >= this.faultyProcesses+1 || totalAnswersCounter >= this.byzantineQuorum) {
                    int consensusInstance = appendMessage.getInstanceOfDecision();
                    boolean success = appendMessage.getSuccess();
                    String outcome = success ? "successfully appended" : "not appended";

                    System.out.println("String " + appendMessage.getValue() +
                            " was " + outcome + " to the blockchain at timestamp " + consensusInstance);
                    System.out.print("> ");
                    appendState.setAppended(true);
                } else {
                    System.out.println("Not reached quorum yet.");
                }
            }
        }
    }

    public PerfectLink getPerfectLink() {
        return perfectLink;
    }
}
