package depchain.client.domain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import depchain.common.*;
import depchain.common.domain.Account;
import depchain.common.domain.Block;
import depchain.common.domain.Entity;
import depchain.common.messaging.*;
import depchain.common.messaging.Message.CoinType;
import depchain.common.messaging.library.AppendMessage;
import depchain.common.messaging.library.BalanceOfMessage;
import depchain.common.messaging.library.TransferMessage;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private long nonce = 0;
    private final String baseDir = System.getProperty("user.dir");
    private final String clientName;
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
    private Account myAccount;
    // {clientName: account}
    private Map<String, Account> accounts;

    public Client(String clientName,
                  int port,
                  List<Entity> members,
                  boolean debug,
                  boolean testEnvironment
    ) {
        this.clientName = clientName;
        this.debug = false;
		this.port = port;
        this.members = members;
        this.faultyProcesses = Math.floorDiv(members.size() - 1, 3);
        this.byzantineQuorum = members.size() - faultyProcesses;
        this.memberResponses = new HashMap<>();
        // TODO hardcoded leader
        this.leaderPort = members.get(0).getPort();
        this.dcLogger = new DCLogger(Client.class, true);
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
        // start a thread to deliver incoming messages
        Thread messageDeliveringThread = new Thread(() -> deliverMessage(messageQueue));
        messageDeliveringThread.start();
        this.assignAccount();
        // start processing user input
        if (!testEnvironment) {
            processUserInput();
        }
    }

    public void assignAccount() {
        // load the genesis file
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path rootDir = currentDir.getParent();
        Path genesisPath = rootDir.resolve("genesis-file.json");
        String jsonString = null;
        try {
            jsonString = Files.readString(genesisPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonElement jsonElement = JsonParser.parseString(jsonString);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Block genesis_block = JsonAdapter.parseBlock(jsonObject);
        //System.out.println("Genesis block: " + genesis_block);

        // load account
        String address = null;
        try {
            address = KeyUtils.hashPublicKey(clientKeys.getPublic());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Client address: " + address);
        this.myAccount = genesis_block.getState().getAccount(address);
        dcLogger.log("Account assigned: " + this.myAccount);
        // stores other accounts
        this.accounts = new HashMap<>();
        for (Account account : genesis_block.getState().getAccounts().values()) {
            //if (!account.getAddress().equals(this.myAccount.getAddress())) {
                this.accounts.put(account.getName(), account);
            //}
        }
        //dcLogger.log("All accounts: " + this.accounts);
    }

    public void stop() {
        perfectLink.stop();
        running = false;
    }

    private void processUserInput() {
        Scanner input = new Scanner(System.in);
        while (running) {
            System.out.print("> ");
            String[] content = input.nextLine().split(" ");
            System.out.print("content: " + Arrays.toString(content) + "\n");
            if (content[0].equalsIgnoreCase("QUIT") || content[0].equalsIgnoreCase("EXIT")) {
                input.close();
                System.exit(0);
            } else if (content[0].equalsIgnoreCase("HELP")) {
                printHelpInfo();
            } else if (content[0].equalsIgnoreCase("ISTCoin")) {
                handleCoinCommand(Arrays.copyOfRange(content, 1, content.length), CoinType.ISTCOIN);
            } else if (content[0].equalsIgnoreCase("DepCoin")) {
                handleCoinCommand(Arrays.copyOfRange(content, 1, content.length), CoinType.DEPCOIN);
            } else {
                System.out.println("Invalid Command.");
                printHelpInfo();
            }
            //sendAppend(content);
        }
    }



    private void handleCoinCommand(String[] content, CoinType coinType) {
        switch(content[0].toUpperCase()) {
            // this is just to help debug, not really important
            case "BALANCE":
                handleBalanceCommand(content, coinType);
                break;
            case "TRANSFER":
                handleTransferCommand(content, coinType);
                break;
            case "APPROVE":
                handleApproveCommand(content, coinType);
                break;
            case "ALLOWANCE":
                handleAllowanceCommand(content, coinType);
                break;
            default:
                System.out.println("Invalid command.");
        }
    }

    private void handleAllowanceCommand(String[] content, CoinType coinType) {
    }

    private void handleApproveCommand(String[] content, CoinType coinType) {

    }

    private void handleTransferCommand(String[] content, CoinType coinType) {
        if (content.length == 3) {
            String nameOfToAddress = content[1];
            dcLogger.verbose("nameOfToAddress: " + nameOfToAddress);
            dcLogger.verbose("accounts: " + accounts);
            if (!accounts.containsKey(nameOfToAddress)) {
                System.out.println("Invalid account address!");
                return;
            }
            String toAddress = accounts.get(nameOfToAddress).getAddress();
            BigInteger amount = BigInteger.valueOf(Long.parseLong(content[2]));
            // TODO check if he has enough balance here and amount (?)
            String fromAddress = this.myAccount.getAddress();
            TransferMessage msg = new TransferMessage(fromAddress, toAddress, amount, coinType, nonce);
            String dataToSign = msg.getDataToSign();
            String signature = Security.makeDS(dataToSign, clientKeys.getPrivate());
            msg.setSignature(signature);
            // send the message to the leader
            perfectLink.sendMessage(msg, leaderPort);
            dcLogger.verbose("Sent message: " + msg);
            incrementNonce();
        } else {
            dcLogger.log("Invalid command. Usage: <CoinType> TRANSFER <address> <amount>");
        }
    }

    private void handleBalanceCommand(String[] content, CoinType coinType) {
        if (content.length == 2) {
            String accName = content[1];
            // TODO -> só podemos verificar o balance da nossa conta?
            if (!accounts.containsKey(accName)) {
                System.out.println("Invalid account address!");
                return;
            }
            String addr = accounts.get(accName).getAddress();
            // broadcast request to members
            BalanceOfMessage msg = new BalanceOfMessage(addr, this.port, coinType);
            broadcastMessage(msg);


        } else {
            System.out.println("Invalid command. Usage: DepCoin BALANCE <address>");
        }
    }

    private void broadcastMessage(Message message) {
        for (Entity member : members) {
            perfectLink.sendMessage(message, member.getPort());
            dcLogger.log("Sent message: " + message);
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

    private void printHelpInfo() {
        System.out.println("-- Available commands: --");
        System.out.println("1. ISTCoin <command> <args>");
        System.out.println("2. DepCoin <command> <args>");
        System.out.println("3. QUIT | EXIT");
        System.out.println("4. HELP");
        System.out.println("-- Commands for ISTCoin and DepCoin (prefix with coin name): --");
        System.out.println("- BALANCE <address>");
        System.out.println("- TRANSFER <address> <amount>");
    }

    private void incrementNonce() {
        this.nonce++;
    }
}
