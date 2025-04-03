package depchain.client.domain;

import depchain.common.*;
import depchain.common.domain.Account;
import depchain.common.domain.Block;
import depchain.common.domain.Entity;
import depchain.common.messaging.*;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.library.*;

import javax.xml.transform.TransformerFactory;
import java.math.BigInteger;
import java.net.DatagramSocket;
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
    // 0x... address for client to send requests to the members
    //private Account myAccount;
    private String myAddress;
    // {clientName: address}
    private Map<String, String> addresses;

    private Map<ClientReplyMessage, Integer> memberReplyMessages;

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

        this.memberReplyMessages = new HashMap<>();
    }

    public void start() throws Exception {
        this.clientKeys = Security.getMemberKeyPair(baseDir, clientName);
        if (clientKeys == null) {
            dcLogger.log("Keys not loaded successfully.");
        }
        // init socket, messageQueue and the perfectLink abstraction
        DatagramSocket socket = new DatagramSocket(port);
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
        assignAddress();
        // start processing user input
        if (!testEnvironment) {
            processUserInput();
        }
    }

    public void assignAddress() {
        // load the genesis file
        Block genesisBlock = CommonUtils.loadGenesisBlock();

        // load account
        String address = null;
        try {
            address = KeyUtils.hashPublicKey(clientKeys.getPublic());
        } catch (Exception e) {
            dcLogger.error("Error hashing public key: " + e.getMessage());
            throw new RuntimeException(e);
        }
        this.myAddress = address;
        dcLogger.verbose("Client address: " + address);
        // sanity check, to see if it exists in the genesis block
        if (genesisBlock.getState() != null &&
            !genesisBlock.getState().getAccounts().containsKey(address)) {
            dcLogger.alert("Account not found in genesis block.");
            return;
        }
        // loads remaining addresses
        this.addresses = new HashMap<>();
        for (Account account : genesisBlock.getState().getAccounts().values()) {
            //if (!account.getAddress().equals(this.myAccount.getAddress())) {
            // TODO -> this should be {address: account}, but it's simpler to type the client name
                this.addresses.put(account.getName(), account.getAddress());
            //}
        }
        dcLogger.verbose("All addresses: " + this.addresses);
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
            //System.out.print("content: " + Arrays.toString(content) + "\n");
            if (content[0].equalsIgnoreCase("QUIT") || content[0].equalsIgnoreCase("EXIT")) {
                input.close();
                System.exit(0);
            } else if (content[0].equalsIgnoreCase("HELP")) {
                printHelpInfo();
            } else if (content[0].equalsIgnoreCase("APPEND")) {
                if (content.length < 2) {
                    System.out.println("Invalid command. Usage: APPEND <string>");
                    continue;
                }
                sendAppend(content[1]);
            } else if (content[0].equalsIgnoreCase("ISTCoin")) {
                if (content.length < 2) {
                    System.out.println("Invalid command. Usage: ISTCoin <command> <args>");
                    continue;
                }
                handleCoinCommand(Arrays.copyOfRange(content, 1, content.length), CoinType.ISTCOIN);
            } else if (content[0].equalsIgnoreCase("DepCoin")) {
                if (content.length < 2) {
                    System.out.println("Invalid command. Usage: DepCoin <command> <args>");
                    continue;
                }
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
            case "TRANSFER_FROM":
                handleTransferFromCommand(content, coinType);
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

    private void handleTransferFromCommand(String[] content, CoinType coinType) {
        if (content.length != 4) {
            dcLogger.log("Invalid command. Usage: <CoinType> TRANSFER_FROM <owner> <to> <amount>");
            return;
        }
        String nameOfOwnerAddr = content[1];
        String nameOfToAddr = content[2];
        BigInteger amount = BigInteger.valueOf(Long.parseLong(content[3]));
        dcLogger.verbose("nameOfOwnerAddr: " + nameOfOwnerAddr);
        dcLogger.verbose("nameOfToAddress: " + nameOfToAddr);
        dcLogger.verbose("addresses: " + addresses);
        if (!addresses.containsKey(nameOfOwnerAddr)) {
            System.out.println("Invalid owner account address!");
            return;
        }
        if (!addresses.containsKey(nameOfToAddr)) {
            System.out.println("Invalid destination account address!");
            return;
        }
        String ownerAddr = addresses.get(nameOfOwnerAddr);
        String toAddr = addresses.get(nameOfToAddr);
        String fromAddress = this.myAddress;
        // the invoker (myself, the client) is the spender
        TransferMessage msg = new TransferMessage(ownerAddr, this.myAddress, toAddr, amount, coinType, nonce, TransactionType.TRANSFER_FROM);
        String dataToSign = msg.getDataToSign();
        dcLogger.verbose("dataToSign: " + dataToSign);
        String signature = Security.makeDS(dataToSign, clientKeys.getPrivate());
        msg.setSignature(signature);
        // send the message to the leader
        perfectLink.sendMessage(msg, leaderPort);
        dcLogger.verbose("Sent message: " + msg);
        incrementNonce();
    }

    private void handleAllowanceCommand(String[] content, CoinType coinType) {
        if (content.length != 2) {
            dcLogger.log("Invalid command. Usage: <CoinType> ALLOWANCE <spender>");
            return;
        }
        String spender = content[1];
        if (!addresses.containsKey(spender)) {
            System.out.println("Invalid account address!");
            return;
        };
        String spenderAddr = addresses.get(spender);
        // TODO -> acho que o spender address sou eu (?)
        AllowanceMessage msg = new AllowanceMessage(this.myAddress, spenderAddr, this.port, coinType);
        broadcastMessage(msg);
    }

    private void handleApproveCommand(String[] content, CoinType coinType) {
        if (content.length != 3) {
            dcLogger.log("Invalid command. Usage: <CoinType> APPROVE <spender> <amount>");
            return;
        }
        String spender = content[1];
        BigInteger amount = BigInteger.valueOf(Long.parseLong(content[2]));
        if (!addresses.containsKey(spender)) {
            System.out.println("Invalid account address!");
            return;
        }
        String spenderAddr = addresses.get(spender);
        TransferMessage msg = new TransferMessage(myAddress, null, spenderAddr, amount, coinType, nonce, TransactionType.APPROVE);
        String dataToSign = msg.getDataToSign();
        String signature = Security.makeDS(dataToSign, clientKeys.getPrivate());
        msg.setSignature(signature);
        perfectLink.sendMessage(msg, leaderPort);
        dcLogger.verbose("Sent message: " + msg);
        incrementNonce();
    }

    private void handleTransferCommand(String[] content, CoinType coinType) {
        if (content.length == 3) {
            String nameOfToAddress = content[1];
            dcLogger.verbose("nameOfToAddress: " + nameOfToAddress);
            dcLogger.verbose("addresses: " + addresses);
            if (!addresses.containsKey(nameOfToAddress)) {
                System.out.println("Invalid account address!");
                return;
            }
            String toAddress = addresses.get(nameOfToAddress);
            BigInteger amount = BigInteger.valueOf(Long.parseLong(content[2]));
            // TODO check if he has enough balance here and amount (?)
            String fromAddress = this.myAddress;
            TransferMessage msg = new TransferMessage(fromAddress, null, toAddress, amount, coinType, nonce, TransactionType.TRANSFER);
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
            if (!addresses.containsKey(accName)) {
                System.out.println("Invalid account address!");
                return;
            }
            String addr = addresses.get(accName);
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
                ClientReplyMessage replyMessage = (ClientReplyMessage) message;
                switch (replyMessage.getReplyType()) {
                    case TRANSFER_REPLY -> handleTransferReply((TransferReply) replyMessage);
                    case BALANCE_REPLY -> handleBalanceReply((BalanceReply) replyMessage);
                    case ALLOWANCE_REPLY -> handleAllowanceReply((AllowanceReply) replyMessage);
                    default -> handleStringReply(replyMessage);
                }
            }
        }
    }

    private void handleStringReply(ClientReplyMessage reply) {
        memberReplyMessages.putIfAbsent(reply, 0);
        memberReplyMessages.put(reply, memberReplyMessages.get(reply) + 1);
        // reached quorum of f+1 equal messages
        if (memberReplyMessages.get(reply) == this.faultyProcesses+1) {
            System.out.println("Server appended String: {");
            System.out.println(reply.getValue());
            System.out.println("}");
        }
        else {
            System.out.println("Not reached quorum yet.");
        }
    }
    private void handleTransferReply(TransferReply reply) {
        memberReplyMessages.putIfAbsent(reply, 0);
        memberReplyMessages.put(reply, memberReplyMessages.get(reply) + 1);
        // reached quorum of f+1 equal messages
        if (memberReplyMessages.get(reply) == this.faultyProcesses+1) {
            System.out.println("Transfer: {");
            System.out.println("From: " + reply.getSenderAddr());
            System.out.println("To: " + reply.getRecipientAddr());
            if (!reply.getSenderAddr().isEmpty()) System.out.println("Spender: " + reply.getSenderAddr());
            System.out.println("Amount: " + reply.getAmount());
            System.out.println("}");
        }
        else {
            System.out.println("Not reached quorum yet.");
        }
    }
    private void handleBalanceReply(BalanceReply reply) {
        memberReplyMessages.putIfAbsent(reply, 0);
        memberReplyMessages.put(reply, memberReplyMessages.get(reply) + 1);
        // reached quorum of f+1 equal messages
        if (memberReplyMessages.get(reply) == this.faultyProcesses+1) {
            System.out.println("Balance: {");
            System.out.println("address "+ reply.getAddress());
            System.out.println("balance: " + reply.getBalance());
            System.out.println("}");
        }
        else {
            System.out.println("Not reached quorum yet.");
        }

    }
    private void handleAllowanceReply(AllowanceReply reply) {
        memberReplyMessages.putIfAbsent(reply, 0);
        memberReplyMessages.put(reply, memberReplyMessages.get(reply) + 1);
        // reached quorum of f+1 equal messages
        if (memberReplyMessages.get(reply) == this.faultyProcesses+1) {
            System.out.println("Allowance {");
            System.out.println("owner: " + reply.getOwner());
            System.out.println("spender: " + reply.getSpender());
            System.out.println("amount: " + reply.getAllowance());
            System.out.println("}");
        }
        else {
            System.out.println("Not reached quorum yet.");
        }
    }

    private void printHelpInfo() {
        System.out.println("-- Available commands: --");
        System.out.println("1. APPEND <string> (compatibility with Delivery 1)");
        System.out.println("2. ISTCoin <command> <args>");
        System.out.println("3. DepCoin <command> <args>");
        System.out.println("4. QUIT | EXIT");
        System.out.println("5. HELP");
        System.out.println("-- Commands for ISTCoin and DepCoin (prefix with coin name): --");
        System.out.println("- BALANCE <address>");
        System.out.println("- TRANSFER <address> <amount>");
        System.out.println("- TRANSFER_FROM <owner> <to> <amount>");
        System.out.println("- APPROVE <spender> <amount>");
        System.out.println("- ALLOWANCE <spender>");
    }

    private void incrementNonce() {
        this.nonce++;
    }
}
