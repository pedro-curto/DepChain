package depchain.client.domain;

import depchain.common.*;
import depchain.common.domain.Account;
import depchain.common.domain.Entity;
import depchain.common.domain.GenesisBlock;
import depchain.common.domain.Transaction;
import depchain.common.messaging.*;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.library.*;

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
    private volatile boolean running = true;
    // 0x... address for client to send requests to the members
    private String myAddress;
    // {clientName: address} (other accounts addresses)
    private Map<String, String> addresses;
    // for tests
    private BigInteger lastBalance;
    private final boolean testEnvironment;
    private TransferReply lastTransferReply;
    private Transaction lastExecutedTransaction;
    private BigInteger lastAllowance;

    private Map<ClientReplyMessage, Integer> memberReplyMessages;

    public Client(String clientName,
                  int port,
                  List<Entity> members,
                  boolean testEnvironment
    ) {
        this.clientName = clientName;
		this.port = port;
        this.members = members;
        this.faultyProcesses = Math.floorDiv(members.size() - 1, 3);
        this.byzantineQuorum = members.size() - faultyProcesses;
        this.memberResponses = new HashMap<>();
        // TODO hardcoded leader
        this.leaderPort = members.getFirst().getPort();
        this.dcLogger = new DCLogger(Client.class, true);
        this.testEnvironment = testEnvironment;

        this.memberReplyMessages = new HashMap<>();
        this.myAddress = null;
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

    public void  assignAddress() {
        // load the genesis file
        GenesisBlock genesisBlock = CommonUtils.loadGenesisBlock();

        // load account
        try {
            this.myAddress = KeyUtils.hashPublicKey(clientKeys.getPublic());
        } catch (Exception e) {
            dcLogger.error("Error hashing public key: " + e.getMessage());
            throw new RuntimeException(e);
        }
        dcLogger.verbose("Client address: " + this.myAddress);
        // sanity check, to see if it exists in the genesis block
        if (genesisBlock != null &&
                genesisBlock.getAccounts()
                .stream()
                .noneMatch(account -> account.getAddress().equals(this.myAddress))) {
            dcLogger.alert("Account not found in genesis block.");
            return;
        }
        // loads remaining addresses
        this.addresses = new HashMap<>();
        for (Account account : genesisBlock.getAccounts()) {
            //if (!account.getAddress().equals(this.myAccount.getAddress())) {
            // this should be {address: account}, but it's simpler to type the client name
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
        }
    }

    private void handleCoinCommand(String[] content, CoinType coinType) {
        switch(content[0].toUpperCase()) {
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
            case "BLACKLIST":
                handleBlacklist(content, coinType);
                break;
            case "UNBLACKLIST":
                handleUnBlacklist(content, coinType);
                break;
            case "ISBLACKLISTED":
                handleIsBlackListed(content, coinType);
            default:
                System.out.println("Invalid command.");
        }
    }

    private void handleIsBlackListed(String[] content, CoinType coinType) {
        if (content.length != 2) {
            System.out.println("Invalid command. Usage: <CoinType> ISBLACKLISTED <address>");
            return;
        }
        String account = content[1];
        if(!addresses.containsKey(account)) {
            System.out.println("Invalid account address!");
        }
        String accountAddr = addresses.get(account);
        IsBlackListedMessage msg = new IsBlackListedMessage(accountAddr, this.port, coinType);
        broadcastMessage(msg);
    }

    private void handleBlacklist(String[] content, CoinType coinType) {
        if (content.length != 2) {
            System.out.println("Invalid command. Usage: <CoinType> BLACKLIST <address>");
            return;
        }
        String nameAddress = content[1];
        if (!addresses.containsKey(nameAddress)) {
            System.out.println("Invalid account address!");
            return;
        }
        String addr = addresses.get(nameAddress);
        TransferMessage msg = new TransferMessage(
                this.myAddress,
                null,
                addr,
                //  TODO have to send BigInteger.ZERO otherwise json can't convert null to BigInteger in parseBlock
                BigInteger.ZERO, // change this in the future
                coinType,
                nonce,
                TransactionType.BLACKLIST,
                this.port
        );
        String dataToSign = msg.getDataToSign();
        String signature = Security.makeDS(dataToSign, clientKeys.getPrivate());
        msg.setSignature(signature);
        sendMessageToLeader(msg);
    }

    private void handleUnBlacklist(String[] content, CoinType coinType) {
        if (content.length != 2) {
            System.out.println("Invalid command. Usage: <CoinType> UNBLACKLIST <address>");
            return;
        }
        String nameAddress = content[1];
        if (!addresses.containsKey(nameAddress)) {
            System.out.println("Invalid account address!");
            return;
        }
        String addr = addresses.get(nameAddress);
        TransferMessage msg = new TransferMessage(
                this.myAddress,
                null,
                addr,
                //  TODO have to send BigInteger.ZERO otherwise json can't convert null to BigInteger in parseBlock
                BigInteger.ZERO, // change this in the future
                coinType,
                nonce,
                TransactionType.UNBLACKLIST,
                this.port
        );
        String dataToSign = msg.getDataToSign();
        String signature = Security.makeDS(dataToSign, clientKeys.getPrivate());
        msg.setSignature(signature);
        sendMessageToLeader(msg);
    }

    private void handleTransferFromCommand(String[] content, CoinType coinType) {
        if (content.length != 4) {
            System.out.println("Invalid command. Usage: <CoinType> TRANSFER_FROM <owner> <to> <amount>");
            return;
        }
        String nameOfOwnerAddr = content[1];
        String nameOfToAddr = content[2];
        BigInteger amount = BigInteger.valueOf(Long.parseLong(content[3]));
        if (!addresses.containsKey(nameOfOwnerAddr)) {
            System.out.println("Invalid owner account address!");
            return;
        }
        if (!addresses.containsKey(nameOfToAddr)) {
            System.out.println("Invalid destination account address!");
            return;
        }
        sendTransferFrom(nameOfOwnerAddr, nameOfToAddr, amount, coinType);
    }

    private void handleAllowanceCommand(String[] content, CoinType coinType) {
        if (content.length != 3) {
            System.out.println("Invalid command. Usage: <CoinType> ALLOWANCE <owner> <spender>");
            return;
        }
        String owner = content[1];
        String spender = content[2];
        if (!addresses.containsKey(spender)) {
            System.out.println("Invalid account address!");
            return;
        }
        sendGetAllowance(owner, spender, coinType);
    }

    private void handleApproveCommand(String[] content, CoinType coinType) {
        if (content.length != 3) {
            System.out.println("Invalid command. Usage: <CoinType> APPROVE <spender> <amount>");
            return;
        }
        String spender = content[1];
        BigInteger amount = BigInteger.valueOf(Long.parseLong(content[2]));
        if (!addresses.containsKey(spender)) {
            System.out.println("Invalid account address!");
            return;
        }
        sendApprove(spender, amount, coinType);
    }

    private void handleTransferCommand(String[] content, CoinType coinType) {
        if (content.length != 3) {
            System.out.println("Invalid command. Usage: <CoinType> TRANSFER <address> <amount>");
            return;
        }
        String nameOfToAddress = content[1];
        BigInteger amount = BigInteger.valueOf(Long.parseLong(content[2]));
        if (!addresses.containsKey(nameOfToAddress)) {
            System.out.println("Invalid account address!");
            return;
        }
        sendTransfer(nameOfToAddress, amount, coinType);
    }

    private void handleBalanceCommand(String[] content, CoinType coinType) {
        if (content.length != 2) {
            System.out.println("Invalid command. Usage: DepCoin BALANCE <address>");
            return;
        }
        String accName = content[1];
        if (!addresses.containsKey(accName)) {
            System.out.println("Invalid account address!");
            return;
        }
        sendGetBalance(accName, coinType);
    }

    /*------------------------------------------------------------------------*/
    /*--------------------------- SENDER FUNCTIONS ---------------------------*/
    /*----- In order for the functions to be callable in test scenarios ------*/
    /*------------------------------------------------------------------------*/

    // nameOfToAddress, content, coinType
    public void sendTransfer(String nameOfToAddress, BigInteger amount, CoinType coinType) {
        String toAddress = addresses.get(nameOfToAddress);
        TransferMessage msg = new TransferMessage(
                this.myAddress,
                "0x",
                toAddress,
                amount,
                coinType,
                nonce,
                TransactionType.TRANSFER,
                this.port
        );
        String signature = generateSignature(msg);
        msg.setSignature(signature);
        sendMessageToLeader(msg);
    }

    public void sendGetBalance(String accName, CoinType coinType) {
        String addr = addresses.get(accName);
        // broadcast request to members
        BalanceOfMessage msg = new BalanceOfMessage(addr, this.port, coinType);
        broadcastMessage(msg);
    }

    public void sendTransferFrom(String nameOfOwnerAddr, String nameOfToAddr, BigInteger amount, CoinType coinType) {
        String ownerAddr = addresses.get(nameOfOwnerAddr);
        String toAddr = addresses.get(nameOfToAddr);
        // the invoker (myself, the client) is the spender
        TransferMessage msg = new TransferMessage(
                ownerAddr,
                this.myAddress,
                toAddr,
                amount,
                coinType,
                nonce,
                TransactionType.TRANSFER_FROM,
                this.port
        );
        String dataToSign = msg.getDataToSign();
        String signature = Security.makeDS(dataToSign, clientKeys.getPrivate());
        msg.setSignature(signature);

        sendMessageToLeader(msg);
    }

    public void sendApprove(String spender, BigInteger amount, CoinType coinType) {
        String spenderAddr = addresses.get(spender);
        TransferMessage msg = new TransferMessage(
                myAddress,
                null,
                spenderAddr,
                amount,
                coinType,
                nonce,
                TransactionType.APPROVE,
                this.port
        );
        String dataToSign = msg.getDataToSign();
        String signature = Security.makeDS(dataToSign, clientKeys.getPrivate());
        msg.setSignature(signature);

        sendMessageToLeader(msg);
    }

    public void sendGetAllowance(String owner, String spender, CoinType coinType) {
        String ownerAddr = addresses.get(owner);
        String spenderAddr = addresses.get(spender);
        AllowanceMessage msg = new AllowanceMessage(ownerAddr, spenderAddr, this.port, coinType);
        broadcastMessage(msg);
    }

    /*------------------------------------------------------------------------*/
    /*--------------------------- HELPER FUNCTIONS ---------------------------*/
    /*------------------------------------------------------------------------*/

    private void broadcastMessage(Message message) {
        for (Entity member : members) {
            perfectLink.sendMessage(message, member.getPort());
            dcLogger.log("Sent message: " + message);
        }
        incrementNonce();
    }

    protected void sendMessageToLeader(Message message) {
        dcLogger.log("Message: " + message);
        perfectLink.sendMessage(message, leaderPort);
        incrementNonce();
        dcLogger.log("Sent message to leader: " + message);
    }

    public void sendAppend(String content) {
        AppendMessage msg = new AppendMessage(content, this.port, nonce);
        String signature = Security.makeDS(msg.getDataToSign(), clientKeys.getPrivate());
        msg.setSignature(signature);
        sendMessageToLeader(msg);
    }

    public String generateSignature(TransferMessage msg) {
        String dataToSign = msg.getDataToSign();
        return Security.makeDS(dataToSign, clientKeys.getPrivate());
    }

    private void deliverMessage(BlockingQueue<Message> messageQueue) {
        while (running) {
            Message message;
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            System.out.println("Delivering " + message.getType() + "...");
            switch (message.getType()) {
                case TRANSFER_REPLY:
                    TransferReply transferReply = (TransferReply) message;
                    handleTransferReply(transferReply);
                    break;
                case BALANCE_REPLY:
                    BalanceReply balanceReply = (BalanceReply) message;
                    handleBalanceReply(balanceReply);
                    break;
                case ALLOWANCE_REPLY:
                    AllowanceReply allowanceReply = (AllowanceReply) message;
                    handleAllowanceReply(allowanceReply);
                    break;
                case STRING_REPLY:
                    if (message instanceof ClientReplyMessage clientReplyMessage) {
                        handleStringReply(clientReplyMessage);
                    } else {
                        System.out.println("Received STRING_REPLY but message is not a ClientReplyMessage instance: " + message);
                    }
                    break;
                    //ClientReplyMessage clientReplyMessage = (ClientReplyMessage) message;
                    //handleStringReply(clientReplyMessage);
                    //break;
                case IS_BLACK_LISTED_REPLY:
                    IsBlackListedReply isBlackListedReply = (IsBlackListedReply) message;
                    handleIsBlackListedReply(isBlackListedReply);
                    break;
                default:
                    System.out.println("Reply type does not exist: " + message.getType());
            }
        }
    }

    private void handleIsBlackListedReply(IsBlackListedReply reply) {
        memberReplyMessages.putIfAbsent(reply, 0);
        memberReplyMessages.put(reply, memberReplyMessages.get(reply) + 1);
        // reached quorum of f+1 equal messages
        if (memberReplyMessages.get(reply) == this.faultyProcesses+1) {
            System.out.println("IsBlacklisted: {");
            System.out.println("account: " + reply.getAccount());
            System.out.println("result: " + reply.isBlackListed());
            System.out.println("success: " + reply.getSuccess());
            System.out.println("}");
        }
        else {
            System.out.println("Not reached quorum yet.");
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
            System.out.println("type: " + reply.getType());
            System.out.println("From: " + reply.getSenderAddr());
            System.out.println("To: " + reply.getRecipientAddr());
            if (!reply.getSenderAddr().isEmpty()) System.out.println("Spender: " + reply.getSenderAddr());
            System.out.println("Amount: " + reply.getAmount());
            System.out.println("success: " + reply.getSuccess());
            System.out.println("}");
            this.lastTransferReply = reply;
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
            System.out.println("success: " + reply.getSuccess());
            System.out.println("}");
            this.lastBalance = reply.getBalance();
            dcLogger.log("Instance of decision: " + reply.getInstanceOfDecision());
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
            System.out.println("success: " + reply.getSuccess());
            System.out.println("}");
            this.lastAllowance = reply.getAllowance();
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
        System.out.println("- ALLOWANCE <owner> <spender>");
        System.out.println("- BLACKLIST <address>");
        System.out.println("- UNBLACKLIST <address>");
        System.out.println("- ISBLACKLISTED <address>");
    }

    private void incrementNonce() {
        this.nonce++;
    }

    public BigInteger getLastBalance() {
        return lastBalance;
    }

    public TransferReply getLastTransferReply() {
        return lastTransferReply;
    }

    public void setLastExecutedTransaction(Transaction tx) {
        this.lastExecutedTransaction = tx;
        dcLogger.log("Last executed transaction: " + tx);
        dcLogger.log("tx signature: " + tx.getSignature());
    }

    public Transaction getLastExecutedTransaction() {
        return lastExecutedTransaction;
    }

    public void setLastTransferReply(TransferReply lastTransferReply) {
        this.lastTransferReply = lastTransferReply;
    }

    public String getClientName() {
        return clientName;
    }

    public void setLastBalance(BigInteger lastBalance) {
        this.lastBalance = lastBalance;
    }

    public BigInteger getLastAllowance() {
        return lastAllowance;
    }

    public void setLastAllowance(BigInteger lastAllowance) {
        this.lastAllowance = lastAllowance;
    }
}
