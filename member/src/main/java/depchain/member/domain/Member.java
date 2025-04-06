package depchain.member.domain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import depchain.common.*;
import depchain.common.domain.*;
import depchain.common.messaging.*;
import depchain.common.messaging.consensus.*;
import depchain.common.messaging.library.*;
import depchain.contract.ContractFunctions;
import depchain.member.state.StringChain;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;

import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

public class Member {
    protected Config config;
    protected final DCLogger dcLogger;
    protected PerfectLink perfectLink;
    // state info
    protected final StringChain stringChain;
    protected BlockChainState blockChainState;
    // queues for message processing
    protected BlockingQueue<Message> messageQueue;
    protected BlockingQueue<TransferMessage> transferQueue = new LinkedBlockingQueue<>();
    private final Map<Integer, Long> clientNonces = new ConcurrentHashMap<>();
    private long serverNonce = 0L;
    private volatile boolean running;
    private boolean caughtInvalidSignature = false;
    private boolean replayAttack = false;
    // smart contract fields
    private EVMExecutor evmExecutor;
    private SimpleWorld world;
    private ByteArrayOutputStream bos;
    // for transfer processing
    private final ScheduledExecutorService blockSched = Executors.newSingleThreadScheduledExecutor();
    private static final int BLOCK_TIMEOUT_SECONDS = 5;
    private static final int MAX_TX_PER_BLOCK = 15;
    private static final int MAX_TX_PER_CLIENT = 5;
    protected ConsensusHandler consensusHandler;

    public Member(Config config,
                  DCLogger dcLogger,
                  PerfectLink pf,
                  ConsensusState cState,
                  StringChain bcState,
                  BlockingQueue<Message> messageQueue) {
       this.config = config;
       this.dcLogger = dcLogger;
       this.perfectLink = pf;
       this.stringChain = bcState;
       this.messageQueue = messageQueue;
       this.consensusHandler = new ConsensusHandler(this, dcLogger, cState);
    }

    public void start() throws Exception {
        if (config.getLeader() == null) {
            dcLogger.error("Leader not found");
            return;
        }
        // evm stuff
        initializeEVM();
        this.running = true;
        startConnections();
        // start the task that will process the transfer messages
        if (this.isLeader()) {
            blockSched.scheduleAtFixedRate(
                    this::processTransferMessages,
                    BLOCK_TIMEOUT_SECONDS,
                    BLOCK_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
        }
        new Thread(this::doConsensus).start();
        // don't put code after receiveMessages because this thread passes onto receiveMessages
        receiveMessages();
    }

    private void initializeEVM() {
        this.world = new SimpleWorld();
        this.bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        StandardJsonTracer tracer = new StandardJsonTracer(ps, true, true, true, true);
        GenesisBlock genesisBlock = CommonUtils.loadGenesisBlock();
        if (genesisBlock == null) {
            throw new RuntimeException("Failed to load genesis block");
        }
        //dcLogger.log("Genesis block: " + genesisBlock);
        // deploy contract
        ContractData contractData = genesisBlock.getContractData();
        String contractAddr = contractData.getAddress();
        String ownerAddr = contractData.getOwnerAddress();
        dcLogger.verbose("Owner: " + ownerAddr);
        this.evmExecutor = ContractFunctions.deployContract(ownerAddr, contractAddr, this.world, tracer, contractData.getDeploymentBytecode(), contractData.getRuntimeBytecode());

        // make some calls to basic info
        ContractFunctions.callName(this.evmExecutor, this.bos);
        ContractFunctions.callSymbol(this.evmExecutor, this.bos);
        ContractFunctions.callTotalSupply(this.evmExecutor, this.bos);
        ContractFunctions.callDecimals(this.evmExecutor, this.bos);

        // prints all balances
        for (Account account: genesisBlock.getAccounts()) {
            Address addr = Address.fromHexString(account.getAddress());
            ContractFunctions.callBalanceOf(this.evmExecutor, this.bos, addr);
        }

        // construct initial blockchain state
        this.blockChainState = new BlockChainState(genesisBlock.getAccounts(), genesisBlock);
    }

    /***
     * Establishes encrypted sessions with other members
     * @throws Exception
     */
    public void startConnections() throws Exception {
        dcLogger.log("Clients: " + config.getClients());
        perfectLink.start();
        // begins encrypted sessions with all member processes running on higher ports
        // others should do the same for this process
        startSessionsWithOtherMembers();
        dcLogger.log("My sessions: " + perfectLink.getSessions());
    }

    /***
     * Handle messages delivered by the perfect link
     */
    public void receiveMessages() {
        while (running) {
            try {
                Message message = messageQueue.take();
                switch (message.getType()) {
                    case APPEND:
                        // does nothing
                        break;
                    case READ:
                        ReadMessage readMessage = (ReadMessage) message;
                        if (readMessage.getConsensusInstance() >= consensusHandler.getConsensusState().getInstance())
                            handleRead(readMessage);
                        break;
                    case STATE:
                        StateMessage stateMessage = (StateMessage) message;
                        if (stateMessage.getConsensusInstance() >= consensusHandler.getConsensusState().getInstance())
                            handleState(stateMessage);
                        break;
                    case COLLECTED:
                        CollectedMessage collectedMessage = (CollectedMessage) message;
                        if (collectedMessage.getConsensusInstance() >= consensusHandler.getConsensusState().getInstance())
                            handleCollected(collectedMessage);
                        break;
                    case WRITE:
                        WriteMessage writeMessage = (WriteMessage) message;
                        if (writeMessage.getConsensusInstance() >= consensusHandler.getConsensusState().getInstance())
                            handleWrite(writeMessage);
                        break;
                    case ACCEPT:
                        AcceptMessage acceptMessage = (AcceptMessage) message;
                        if (acceptMessage.getConsensusInstance() >= consensusHandler.getConsensusState().getInstance())
                            handleAccept(acceptMessage);
                        break;
                    case TRANSFER:
                        TransferMessage transferMessage = (TransferMessage) message;
                        dcLogger.log("Received: " + transferMessage);
                        // TODO check signature before incrementing nonce??
                        synchronized (this) {
                            // synchronized for avoiding concurrent modification/checking
                            if (isValidClientNonce(transferMessage.getNonce(), transferMessage.getClientPort())) {
                                // TODO -> acho que não devia ser logo
                                setClientNonce(transferMessage.getNonce(), transferMessage.getClientPort());
                                this.transferQueue.put(transferMessage);
                            } else {
                                dcLogger.verbose("Got client( " + transferMessage.getClientPort() + ") nonce: " +
                                        transferMessage.getNonce() + " but current: "
                                        + getClientNonce(transferMessage.getClientPort()));
                                // sends an answer back saying it was rejected
                                TransferReply txReply = new TransferReply(
                                        false,
                                        -1,
                                        transferMessage.getValue(),
                                        transferMessage.getFrom(),
                                        transferMessage.getSpender(),
                                        transferMessage.getTo(),
                                        transferMessage.getCoinType(),
                                        transferMessage.getTransactionType(),
                                        this.serverNonce,
                                        config.getPort()
                                );
                                String signature = Security.makeDS(txReply.getDataToSign(), Security.getMyPrivateKey(config.getMyName()));
                                txReply.setSignature(signature);
                                sendToClient(txReply, transferMessage.getClientPort());
                                this.replayAttack = true;
                            }
                        }
                        break;
                    case BALANCE_OF:
                        BalanceOfMessage balanceOfMessage = (BalanceOfMessage) message;
                        handleBalanceMessage(balanceOfMessage);
                        break;
                    case ALLOWANCE:
                        AllowanceMessage allowanceMessage = (AllowanceMessage) message;
                        handleAllowanceMessage(allowanceMessage);
                        break;
                    case IS_BLACK_LISTED:
                        IsBlackListedMessage isBlackListedMessage = (IsBlackListedMessage) message;
                        handleIsBlackListedMessage(isBlackListedMessage);
                        break;
                    default:
                        dcLogger.log("Unknown message type");
                }
            } catch (InterruptedException e) {
                dcLogger.error("Error while processing message: " + e.getMessage());
            }
        }
    }

    private void processTransferMessages() {
        // aggregates transfers from queue
        List<TransferMessage> tmp = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();
        transferQueue.drainTo(tmp);
        if (tmp.isEmpty()) {
            dcLogger.verbose("No transfer messages to process");
            return;
        }
        // pre-select transactions, making sure there's a max per block and per client
        Map<String, Integer> clientTxs = new HashMap<>();
        List<TransferMessage> acceptedTxs = new ArrayList<>();
        for (TransferMessage msg : tmp) {
            int currentCount = clientTxs.getOrDefault(msg.getFrom(), 0);
            if (acceptedTxs.size() < MAX_TX_PER_BLOCK
                    && currentCount < MAX_TX_PER_CLIENT) {
                acceptedTxs.add(msg);
                clientTxs.put(msg.getFrom(), currentCount + 1);
            } else {
                // builds a reply rejecting the transaction
                TransferReply txReply = new TransferReply(
                        false,
                        -1,
                        msg.getValue(),
                        msg.getFrom(),
                        msg.getSpender(),
                        msg.getTo(),
                        msg.getCoinType(),
                        msg.getTransactionType(),
                        this.serverNonce,
                        config.getPort()
                );
                String signature = Security.makeDS(txReply.getDataToSign(), Security.getMyPrivateKey(config.getMyName()));
                txReply.setSignature(signature);
                sendToClient(txReply, msg.getClientPort());
            }
        }
        // sort the accepted txs based on nonces
        acceptedTxs.sort(Comparator.comparingLong(TransferMessage::getNonce));
        for (TransferMessage msg : acceptedTxs) {
//            if (!Security.validateTransferMessage(msg)) {
//                dcLogger.alert("Signature for transfer message is invalid: " + msg);
//                continue;
//            }
            Transaction tx = new Transaction(
                    msg.getFrom(),
                    msg.getSpender(),
                    msg.getTo(),
                    msg.getValue(),
                    msg.getSignature(),
                    msg.getNonce(),
                    msg.getTransactionType(),
                    msg.getCoinType(),
                    msg.getClientPort()
            );
            transactions.add(tx);
        }
        // constructs block: prevHash, txs, blockNum, ts (he calculates hash on constructor)
        Block lastBlock = this.blockChainState.getLastBlock();
        String prevHash = lastBlock.getHash();
        long timestamp = System.currentTimeMillis();
        Block block = new Block(prevHash, transactions, lastBlock.getBlockNumber()+1, timestamp);
        // add valts to consensus
        ValueTimestampPair newValts = new ValueTimestampPair(0, block);
        consensusHandler.addValtsForConsensus(newValts);
    }

//    public void handleAppend(AppendMessage appendMessage) {
//        long lastClientNonce = getClientNonce(appendMessage.getPort());
//        if (appendMessage.getNonce() <= lastClientNonce) {
//            dcLogger.alert("Received replayed message");
//            return;
//        }
//        ValueTimestampPair newValue = new ValueTimestampPair(
//                0,
//                new ConsensusString(appendMessage.getValue()),
//                appendMessage.getPort(),
//                appendMessage.getNonce()
//        );
//        newValue.setClientSignature(appendMessage.getSignature());
//        consensusHandler.addValtsForConsensus(newValue);
//    }

    public void handleRead(ReadMessage readMessage) {
        //dcLogger.log("Received: " + readMessage);
        ConsensusState state = consensusHandler.getConsensusState();
        // sign: current||writeset||instance||epoch
        String dataToSign = state.getDataToSign();
        //String dataToSign = state.getCurrent().toString() + state.getWriteset() + state.getInstance() + state.getEpoch();
        String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(config.getMyName()));
        dcLogger.log("dataToSign: " + dataToSign);
        // don't send own instance of consensus state to message, send a copy or else stack overflow error
        ConsensusState myState = new ConsensusState(config.getMyName(), state.getCurrent(), state.getWriteset());
        // TODO -> i used the setter here to not change the constructor, use the constructor later
        myState.setInstance(state.getInstance());
        StateMessage stateMessage = new StateMessage(myState, mySignature, state.getInstance(), config.getPort());
        sendToLeader(stateMessage);
    }

    public void handleState(StateMessage stateMessage) {
        //dcLogger.log("Received: " + stateMessage);
        if (isLeader()) {
            ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusHandler.getConsensusState();
            leaderState.addMemberState(stateMessage);
        } else {
            dcLogger.log("[ERROR] Received state message but not the leader");
        }
    }

    public void handleCollected(CollectedMessage collectedMessage) {
        //dcLogger.log("Received: " + collectedMessage);
        if (collectedMessage.getPort() != config.getLeader().getPort()) {
            return;
        }
        consensusHandler.getConsensusState().addCollectedMessage(collectedMessage);
    }

    public void handleWrite(WriteMessage writeMessage) {
        //dcLogger.log("Received: " + writeMessage);
        consensusHandler.getConsensusState().addWriteMessage(writeMessage);
    }

    public void handleAccept(AcceptMessage acceptMessage) {
        //dcLogger.log("Received: " + acceptMessage);
        consensusHandler.getConsensusState().addAcceptMessage(acceptMessage);
    }

    private void handleBalanceMessage(BalanceOfMessage balanceOfMessage) {
        Address addr = Address.fromHexString(balanceOfMessage.getAddress());
        BigInteger balance = BigInteger.ZERO;
        if (balanceOfMessage.getCoinType() == CoinType.ISTCOIN) {
            balance = ISTCoinHandler.handleBalance(addr, this.evmExecutor, this.bos);
        } else if (balanceOfMessage.getCoinType() == CoinType.DEPCOIN) {
            balance = DepCoinHandler.handleBalance(addr, this.blockChainState, this.dcLogger);
        }
        dcLogger.log("Balance of " + addr + ": " + balance);

        BalanceReply balanceReply = new BalanceReply(
                true,
                consensusHandler.getConsensusState().getInstance(),
                balanceOfMessage.getAddress(),
                balance,
                balanceOfMessage.getCoinType(),
                this.serverNonce,
                this.config.getPort()
        );
        String signature = Security.makeDS(balanceReply.getDataToSign(), Security.getMyPrivateKey(config.getMyName()));
        balanceReply.setSignature(signature);

        sendToClient(balanceReply, balanceOfMessage.getPort());
    }

    private void handleAllowanceMessage(AllowanceMessage allowanceMessage) {
        dcLogger.verbose("Received: " + allowanceMessage);
        Address owner = Address.fromHexString(allowanceMessage.getOwner());
        Address spender = Address.fromHexString(allowanceMessage.getSpender());
        BigInteger allowance = BigInteger.ZERO;

        if (allowanceMessage.getCoinType() == CoinType.ISTCOIN) {
            allowance = ISTCoinHandler.handleAllowance(owner, spender, this.evmExecutor, this.bos);
        } else if (allowanceMessage.getCoinType() == CoinType.DEPCOIN) {
            allowance = DepCoinHandler.handleAllowance(owner, spender, this.blockChainState, this.dcLogger);
        } else {
            dcLogger.log("Unknown coin type");
        }
        dcLogger.log("[" + allowanceMessage.getCoinType() + "] Allowance of " + owner + " to " + spender + ": " + allowance);

        AllowanceReply allowanceReply = new AllowanceReply(
                true,
                consensusHandler.getConsensusState().getInstance(),
                allowanceMessage.getOwner(),
                allowanceMessage.getSpender(),
                allowance,
                allowanceMessage.getCoinType(),
                this.serverNonce,
                this.config.getPort()
        );
        String signature = Security.makeDS(allowanceReply.getDataToSign(), Security.getMyPrivateKey(config.getMyName()));
        allowanceReply.setSignature(signature);

        sendToClient(allowanceReply, allowanceMessage.getPort());
    }

    private void handleIsBlackListedMessage(IsBlackListedMessage isBlackListedMessage) {
        dcLogger.verbose("Received: " + isBlackListedMessage);
        Address accountToCheck = Address.fromHexString(isBlackListedMessage.getAccount());
        boolean result = false;
        if (isBlackListedMessage.getCoinType() == CoinType.ISTCOIN) {
            result = ISTCoinHandler.handleIsBlackListed(accountToCheck, this.evmExecutor, this.bos);
        } else if (isBlackListedMessage.getCoinType() == CoinType.DEPCOIN) {
            dcLogger.error("IsBlacklisted not implemented in DepCoin");
        }
        IsBlackListedReply isBlackListedReply = new IsBlackListedReply(
                true,
                consensusHandler.getConsensusState().getInstance(),
                isBlackListedMessage.getAccount(),
                result,
                isBlackListedMessage.getCoinType(),
                this.serverNonce,
                this.config.getPort()
        );
        String signature = Security.makeDS(isBlackListedReply.getDataToSign(), Security.getMyPrivateKey(config.getMyName()));
        isBlackListedReply.setSignature(signature);

        sendToClient(isBlackListedReply, isBlackListedMessage.getPort());
    }

    public void doConsensus() {
        while (running) {
            if (config.getLeader().getPort() != config.getPort()) {
                // if not the leader, this thread will only be responsible for
                if(!consensusHandler.startConsensusMember()) {
                    // aborted
                    consensusHandler.getConsensusState().nextEpoch();
                }
            } else {
                ValueTimestampPair newValts = consensusHandler.getNextValtsForConsensus();
                // leader must keep the consensus going until a
                // value is appended to the blockchain
                boolean finished = false;
                while (!finished) {
                    // checks for replays
                    // TODO -> check client signature here?
                    ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusHandler.getConsensusState();
                    finished = consensusHandler.startConsenusLeader(newValts ,leaderState);
                    if (!finished) {
                        consensusHandler.getConsensusState().nextEpoch();
                    }
                }
            }
            consensusHandler.getConsensusState().nextInstance();
        }
    }

    private void handleTransactions(List<Transaction> transactions) {
        for (Transaction tx : transactions) {
            // start by validating tx, setting success as false if not valid
            if (!Security.validateTransaction(tx)) {
                tx.setStatus(false);
                dcLogger.error("Signature for transaction is invalid: " + tx);
                continue;
            }
            boolean result = false;
            if (tx.getCoinType() == CoinType.ISTCOIN) {
                result = ISTCoinHandler.handleTransaction(tx, this.evmExecutor, this.bos);
            } else if (tx.getCoinType() == CoinType.DEPCOIN) {
                result = DepCoinHandler.handleTransaction(tx, this.blockChainState, this.dcLogger);
            } else {
                dcLogger.log("Unknown coin type");
            }
            tx.setStatus(result);
            if (result) {
                dcLogger.verbose("Transfer successful");
            } else {
                dcLogger.verbose("Transfer failed");
            }
        }
    }

    public void replyTransactions(List<Transaction> transactions) {
        for(Transaction tx: transactions) {
            // TODO could make a TransferReply constructor with a Transaction as argument
            TransferReply transferReply = new TransferReply(
                    tx.getSuccess(),
                    consensusHandler.getConsensusState().getInstance(),
                    tx.getAmount(),
                    tx.getSender(),
                    tx.getSpender(),
                    tx.getRecipient(),
                    tx.getCoinType(),
                    tx.getTransactionType(),
                    this.serverNonce,
                    config.getPort()
            );
            String signature = Security.makeDS(transferReply.getDataToSign(), Security.getMyPrivateKey(config.getMyName()));
            transferReply.setSignature(signature);

            dcLogger.log("SENDING " + transferReply.getType() + "!!!");
            sendToClient(transferReply, tx.getClientPort());
        }
    }

    public void executeTransactions(Block block) {
        dcLogger.log("Executing transactions in block: " + block);
        // update transaction results (if we use the previous ref of blockJson, all tx's status are false)
        handleTransactions(block.getTransactions());
        saveBlock(block);
    }

    private void saveBlock(Block block) {
        JsonObject blockJson = JsonAdapter.serializeBlock(block);
        boolean result = CommonUtils.saveJsonToFile(blockJson, config.getBlockDir() + "/block" + block.getBlockNumber() + ".json");
        if (result) {
            dcLogger.log("Block " + block.getBlockNumber() + " saved successfully");
        } else {
            dcLogger.log("Failed to save block " + block.getBlockNumber());
        }
    }

    public boolean verifyMemberStateAuthenticity(StateMessage stateMessage) {
        ConsensusState consensusSt = stateMessage.getState();
        String memberName = consensusSt.getMemberName();
        PublicKey memberPubKey = Security.getMembershipPublicKey(memberName);
        String reconstructSignatureData = consensusSt.getDataToSign();
        dcLogger.log("Reconstructed signature data: " + reconstructSignatureData);
        //String reconstructSignatureData = consensusSt.getCurrent().toString() + consensusSt.getWriteset() + consensusSt.getInstance() + consensusSt.getEpoch();
        return Security.verifyDS(stateMessage.getSignature(), reconstructSignatureData, memberPubKey);
    }

    public boolean checkClientSignature(ValueTimestampPair leaderVts) {
        String clientSignature = leaderVts.getClientSignature();
        if (clientSignature == null) return false;
        dcLogger.log("Leader Vts: " + leaderVts);
        String reconstructSignatureData = leaderVts.getValue().toString() + leaderVts.getNonce();
        Entity client = config.getClients().stream()
                .filter(c -> c.getPort() == leaderVts.getClientPort())
                .findFirst()
                .orElse(null);
        if (client == null) {
            dcLogger.error("Client not found");
            return false;
        }
        PublicKey clientPubKey = Security.getMembershipPublicKey(client.getEntityName());
        return Security.verifyDS(clientSignature, reconstructSignatureData, clientPubKey);
    }

    public void sendToLeader(Message message) {
        dcLogger.log("Sending to leader " + message.getType() + " message...");
        if (config.getPort() != config.getLeader().getPort())
            perfectLink.sendMessage(message, config.getLeader().getPort());
        else {
            try {
                this.messageQueue.put(message);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendToMe(Message message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendToMember(Message message, int port) {
        dcLogger.log("Sending " + message.getType() + " message... to member " + port);
        if (config.getPort() != port) {
            perfectLink.sendMessage(message, port);
        } else {
            sendToMe(message);
        }
    }

    public void broadCastMessage(Message message) {
        for (Entity member : config.getMembers()) {
            if (member.getPort() == config.getPort()) {
                sendToMe(message);
                continue;
            }
            perfectLink.sendMessage(message, member.getPort());
        }
    }

    public void sendToClient(Message message, int port) {
        dcLogger.log("Sending " + message.getType() + " message... to client " + port);
        perfectLink.sendMessage(message, port);
        incrementServerNonce();
    }

    public void broadCastToClients(Message message) {
        dcLogger.log("BroadCasting to clients " + message.getType() + " message...");
        for (Entity client : config.getClients()) {
            dcLogger.log("[" + message.getType() + " MESSAGE]: " + client.getEntityName());
            perfectLink.sendMessage(message, client.getPort());
            incrementServerNonce();
        }
    }

    public boolean isInitializer(int port) {
        return port > config.getPort();
    }

    public void startSessionsWithOtherMembers() {
        for (Entity otherMember : config.getMembers()) {
            if (isInitializer(otherMember.getPort())) {
                dcLogger.log("Starting session with " + otherMember.getEntityName());
                perfectLink.startSession(otherMember.getPort());
            }
        }
    }

    public boolean isLeader() {
        if (config.getLeader() == null) {
            return false;
        }
        return config.getLeader().getEntityName().equalsIgnoreCase(config.getMyName());
    }

    public void stop() {
        running = false;
        perfectLink.stop();
        blockSched.shutdownNow();
    }

    private long getClientNonce(Integer port) {
        return this.clientNonces.getOrDefault(port, -1L);
    }

    private void setClientNonce(long nonce, int port) {
        if (isValidClientNonce(nonce, port)) {
            clientNonces.put(port, nonce);
        }
    }

    private boolean isValidClientNonce(long nonce, int port) {
        return nonce > getClientNonce(port);
    }

    private void incrementServerNonce() {
        this.serverNonce++;
    }

    public boolean caughtInvalidSignature() {
        return caughtInvalidSignature;
    }

    public void setCaughtInvalidSignature() {
        this.caughtInvalidSignature = true;
    }

    public String getName() {
        return config.getMyName();
    }

    public Config getConfig() {
        return config;
    }

    public StringChain getStringChain() {
        return stringChain;
    }

    public BlockChainState getBlockChainState() {
        return blockChainState;
    }

    public boolean getReplayAttack() {
        return replayAttack;
    }

    public void setLastBlock(Block block){
        blockChainState.setLastBlock(block);
    }
}