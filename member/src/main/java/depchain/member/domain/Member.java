package depchain.member.domain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import depchain.common.*;
import depchain.common.domain.*;
import depchain.common.domain.Transaction.TransactionType;
import depchain.common.messaging.*;
import depchain.common.messaging.consensus.*;
import depchain.common.messaging.library.*;
import depchain.contract.ContractFunctions;
import depchain.member.state.StringChain;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

public class Member {
    protected Config config;
    protected final DCLogger dcLogger;
    protected PerfectLink perfectLink;
    // state info
    protected ConsensusState consensusState;
    protected final StringChain stringChain;
    protected BlockChainState blockChainState;
    // queues for message processing
    protected BlockingQueue<Message> messageQueue;
    protected BlockingQueue<AppendMessage> appendQueue;
    protected BlockingQueue<TransferMessage> transferQueue = new LinkedBlockingQueue<>();
    private Map<Integer, Long> clientNonces = new ConcurrentHashMap<>();
    private volatile boolean running;
    private boolean caughtInvalidSignature = false;
    // smart contract fields
    private EVMExecutor evmExecutor;
    private SimpleWorld world;
    private ByteArrayOutputStream bos;
    // for transfer processing
    private final ScheduledExecutorService blockSched = Executors.newSingleThreadScheduledExecutor();
    private static final int BLOCK_TIMEOUT_SECONDS = 5;

    public Member(Config config,
                  DCLogger dcLogger,
                  PerfectLink pf,
                  ConsensusState cState,
                  StringChain bcState,
                  BlockingQueue<Message> messageQueue,
                  BlockingQueue<AppendMessage> appendQueue) {
       this.config = config;
       this.dcLogger = dcLogger;
       this.perfectLink = pf;
       this.consensusState = cState;
       this.stringChain = bcState;
       this.messageQueue = messageQueue;
       this.appendQueue = appendQueue;
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
        blockSched.scheduleAtFixedRate(
                this::processTransferMessages,
                BLOCK_TIMEOUT_SECONDS,
                BLOCK_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
        new Thread(this::doConsensus).start();
        // don't put code after receiveMessages because this thread passes onto receiveMessages
        receiveMessages();
    }

    private void initializeEVM() {
        this.world = new SimpleWorld();
        this.bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        StandardJsonTracer tracer = new StandardJsonTracer(ps, true, true, true, true);
        // load accounts
        JsonObject rootJson = CommonUtils.getGenesisJsonObject();
        JsonObject stateObject = CommonUtils.jsonGetter(rootJson, "state");
        JsonArray accountsArray = stateObject.getAsJsonArray("accounts");
        List<Account> accounts = new ArrayList<>();
        boolean first = true;
        for (JsonElement accountEl : accountsArray) {
            JsonObject accountObj = accountEl.getAsJsonObject();
            String accountAddrStr = accountObj.get("address").getAsString();
            String name = accountObj.get("name").getAsString();
            long balance = accountObj.get("balance").getAsLong();
            // create address and balance to give to MutableAccount
            Address accountAddr = Address.fromHexString(accountAddrStr);
            Wei balanceWei = Wei.fromEth(balance);
            dcLogger.log("Loading account with address: " + accountAddr + " and balance: " + balanceWei);
            Account account = new Account(accountAddrStr, balance);
            accounts.add(account);
            // TODO -> get a better condition for the blacklist owner?
            if (first) {
                // deploys the contract with himself as sender
                dcLogger.log("Deploying contract...");
                JsonObject contractObj = CommonUtils.jsonGetter(stateObject, "contract");
                String contractAddr = contractObj.get("address").getAsString();
                dcLogger.log("Contract address: " + contractAddr);
                this.evmExecutor = ContractFunctions.deployContract(accountAddrStr, contractAddr, this.world, tracer);
                first = false;
                continue; // avoids creating the account again
            }
            // TODO -> if I do this it's indifferent because every account starts at 0, am i doing it right?
            // add account to world
            //this.world.createAccount(accountAddr, 0, balanceWei);
            // create account
        }
        // make some calls to basic info
        ContractFunctions.callName(this.evmExecutor, this.bos);
        ContractFunctions.callSymbol(this.evmExecutor, this.bos);
        ContractFunctions.callTotalSupply(this.evmExecutor, this.bos);
        ContractFunctions.callDecimals(this.evmExecutor, this.bos);
        // construct initial blockchain state
        Block genesisBlock = CommonUtils.loadGenesisBlock();
        if (genesisBlock == null) {
            throw new RuntimeException("Failed to load genesis block");
        }
        this.blockChainState = new BlockChainState(accounts, genesisBlock);
        // prevHash (null), transactions (empty), blockNumber (1), timestamp (?)
        //String hash = CommonUtils.jsonGetter(rootJson, "hash").getAsString();
        //long timestamp = System.currentTimeMillis();
        //// TODO -> get the timestamp from the genesis file
        //Block genesisBlock = new Block(null, new ArrayList<>(), 1, timestamp);
        //this.blockChainState = new BlockChainState(accounts, genesisBlock);
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
                        AppendMessage appendMessage = (AppendMessage) message;
                        this.appendQueue.put(appendMessage);
                        break;
                    case READ:
                        ReadMessage readMessage = (ReadMessage) message;
                        if (readMessage.getConsensusInstance() == this.consensusState.getInstance())
                            handleRead(readMessage);
                        break;
                    case STATE:
                        StateMessage stateMessage = (StateMessage) message;
                        if (stateMessage.getConsensusInstance() == this.consensusState.getInstance())
                            handleState(stateMessage);
                        break;
                    case COLLECTED:
                        CollectedMessage collectedMessage = (CollectedMessage) message;
                        if (collectedMessage.getConsensusInstance() == this.consensusState.getInstance())
                            handleCollected(collectedMessage);
                        break;
                    case WRITE:
                        WriteMessage writeMessage = (WriteMessage) message;
                        if (writeMessage.getConsensusInstance() == this.consensusState.getInstance())
                            handleWrite(writeMessage);
                        break;
                    case ACCEPT:
                        AcceptMessage acceptMessage = (AcceptMessage) message;
                        if (acceptMessage.getConsensusInstance() == this.consensusState.getInstance())
                            handleAccept(acceptMessage);
                        break;
                    case TRANSFER:
                        TransferMessage transferMessage = (TransferMessage) message;
                        this.transferQueue.put(transferMessage);
                        // TODO -> remove below
                        handleTransfer(transferMessage);
                        break;
                    case BALANCE_OF:
                        BalanceOfMessage balanceOfMessage = (BalanceOfMessage) message;
                        handleBalanceMessage(balanceOfMessage);
                        break;
                    case ALLOWANCE:
                        AllowanceMessage allowanceMessage = (AllowanceMessage) message;
                        handleAllowanceMessage(allowanceMessage);
                        break;
                    case APPROVE:
                        ApproveMessage approveMessage = (ApproveMessage) message;
                        handleApproveMessage(approveMessage);
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
        // TODO is this resilient to concurrency? (draining the transfer queue while another request comes and tries to add)
        List<TransferMessage> tmp = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();
        transferQueue.drainTo(tmp);
        if (tmp.isEmpty()) {
            dcLogger.verbose("No transfer messages to process");
            return;
        }
        for (TransferMessage msg : tmp) {
            if (!Security.validateTransferMessage(msg)) {
                dcLogger.log("Signature for transfer message is invalid: " + msg);
                continue;
            }
            Transaction tx = new Transaction(msg.getFrom(), msg.getTo(), msg.getValue(),
                    msg.getSignature(), msg.getNonce(), TransactionType.TRANSFER, msg.getCoinType());
            transactions.add(tx);

        }
        // constructs block: prevHash, txs, blockNum, ts (he calculates hash on constructor)
        // TODO -> how to compute hash of new block?
        Block lastBlock = this.blockChainState.getLastBlock();
        String prevHash = lastBlock.getHash();
        long timestamp = System.currentTimeMillis();
        Block block = new Block(prevHash, transactions, lastBlock.getBlockNumber()+1, timestamp);
        this.blockChainState.setLastBlock(block);
        // serialize it
        JsonObject blockJson = JsonAdapter.serializeBlock(block);
        boolean result = CommonUtils.saveJsonToFile(blockJson, config.getBlockDir() + "/block" + block.getBlockNumber() + ".json");
        if (result) {
            dcLogger.log("Block " + block.getBlockNumber() + " saved successfully");
        } else {
            dcLogger.log("Failed to save block " + block.getBlockNumber());
        }
    }


    private void handleBalanceMessage(BalanceOfMessage balanceOfMessage) {
        Address addr = Address.fromHexString(balanceOfMessage.getAddress());
        BigInteger balance = ContractFunctions.callBalanceOf(this.evmExecutor, this.bos, addr);
        dcLogger.log("Balance of " + addr + ": " + balance);
        // TODO -> send balance to client
    }

    private void handleTransfer(TransferMessage transferMessage) {
        dcLogger.log("Received transfer message: " + transferMessage);
        if (transferMessage.getCoinType() == CoinType.ISTCOIN) {
            handleISTCoinTransfer(transferMessage);
            return;
        } else if (transferMessage.getCoinType() == CoinType.DEPCOIN) {
            handleDepCoinTransfer(transferMessage);
            return;
        }
        dcLogger.log("Unrecognized coin type");
    }

    private void handleISTCoinTransfer(TransferMessage transferMessage) {
        Address from = Address.fromHexString(transferMessage.getFrom());
        Address to = Address.fromHexString(transferMessage.getTo());
        ContractFunctions.callBalanceOf(evmExecutor, bos, from);
        ContractFunctions.callBalanceOf(evmExecutor, bos, to);
        BigInteger value = transferMessage.getValue();
        boolean result = ContractFunctions.transferTokens(evmExecutor, bos, from, to, value);
        if (result) {
            dcLogger.log("Transfer successful");
        } else {
            dcLogger.log("Transfer failed");
        }
        ContractFunctions.callBalanceOf(evmExecutor, bos, from);
        ContractFunctions.callBalanceOf(evmExecutor, bos, to);
        // serializes transaction to json
        //Transaction transaction = new Transaction(transferMessage.getFrom(), transferMessage.getTo(), value,
        //        transferMessage.getSignature(), transferMessage.getNonce(), TransactionType.TRANSFER);
        //transaction.save();
    }

    private void handleDepCoinTransfer(TransferMessage transferMessage) {
        BigInteger value = transferMessage.getValue();
        Account fromAccount = this.blockChainState.getAccount(transferMessage.getFrom());
        Account toAccount = this.blockChainState.getAccount(transferMessage.getTo());
        if (fromAccount.getBalance().compareTo(value) < 0) {
            dcLogger.log("Not enough balance");
            return;
        }
        if (value == null || value.compareTo(BigInteger.ZERO) <= 0) {
            dcLogger.log("Invalid value");
            return;
        }
        fromAccount.decreaseBalance(value);
        toAccount.increaseBalance(value);
		dcLogger.verbose("New balances:" );
        dcLogger.verbose(fromAccount + ": " + fromAccount.getBalance());
        dcLogger.verbose(toAccount + ": " + toAccount.getBalance());
    }

    public void handleRead(ReadMessage readMessage) {
        dcLogger.log("Received: " + readMessage);
        // sign: current||writeset||instance||epoch
        String dataToSign = consensusState.getCurrent().toString() + consensusState.getWriteset() + consensusState.getInstance() + consensusState.getEpoch();
        dcLogger.log("Signing data: " + dataToSign);
        String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(config.getMyName()));
        // don't send own instance of consensus state to message, send a copy or else stack overflow error
        ConsensusState myState = new ConsensusState(config.getMyName(), consensusState.getCurrent(), consensusState.getWriteset());
        // TODO -> i used the setter here to not change the constructor, use the constructor later
        myState.setInstance(consensusState.getInstance());
        StateMessage stateMessage = new StateMessage(myState, mySignature, consensusState.getInstance(), config.getPort());
        sendToLeader(stateMessage);
    }

    public void handleState(StateMessage stateMessage) {
        dcLogger.log("Received: " + stateMessage);
        if (isLeader()) {
            ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
            leaderState.addMemberState(stateMessage);
        } else {
            dcLogger.log("[ERROR] Received state message but not the leader");
        }
    }

    public void handleCollected(CollectedMessage collectedMessage) {
        dcLogger.log("Received: " + collectedMessage);
        if (collectedMessage.getPort() != config.getLeader().getPort()) {
            return;
        }
        consensusState.addCollectedMessage(collectedMessage);
    }

    public void handleWrite(WriteMessage writeMessage) {
        dcLogger.log("Received: " + writeMessage);
        consensusState.addWriteMessage(writeMessage);
    }

    public void handleAccept(AcceptMessage acceptMessage) {
        dcLogger.log("Received: " + acceptMessage);
        consensusState.addAcceptMessage(acceptMessage);
    }

    private void handleApproveMessage(ApproveMessage approveMessage) {
        dcLogger.verbose("Received: " + approveMessage);
        dcLogger.verbose("Not implemented yet");
    }

    private void handleAllowanceMessage(AllowanceMessage allowanceMessage) {
        dcLogger.verbose("Received: " + allowanceMessage);
        dcLogger.verbose("Not implemented yet");
    }


    /***
     * Waits for new append requests and starts consensus to add them in the blockchain
     */
    public void doConsensus() {
        while (running) {
            if (config.getLeader().getPort() != config.getPort()) {
                // if not the leader, this thread will only be responsible for
                // deciding values to write as it receives COLLECT messages
                // from the main thread
                decideValue();
                continue;
            }
            try {
                AppendMessage appendMessage = appendQueue.take();
                // leader must keep the consensus going until a
                // value is appended to the blockchain
                boolean finished = false;
                while (!finished) {
                    startConsensus(appendMessage);
                    finished = decideValue();
                }
            } catch (InterruptedException e) {
                dcLogger.log("Consensus thread was interrupted while waiting for append messages");
            }
        }
    }

    /***
     * Conditional Collect part of the algorithm
     * @param appendMessage - message with value to be proposed if epoch 0
     */
    public void startConsensus(AppendMessage appendMessage) {
        // checks for replays
        // TODO -> check client signature here?
        long lastClientNonce = clientNonces.getOrDefault(appendMessage.getPort(), -1L);
        if (appendMessage.getNonce() <= lastClientNonce) {
            dcLogger.alert("Received replayed message");
            return;
        }
        dcLogger.log("Received: " + appendMessage);
        dcLogger.log("-- STARTING CONSENSUS FOR '" + appendMessage.getValue() + "' --");

        // if I am the leader and this is the first epoch I should propose the value
        // of the message
        ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
        if (consensusState.getCurrent().getValue().isEmpty()) {
            ValueTimestampPair newValue = new ValueTimestampPair(0, appendMessage.getValue(),
                    appendMessage.getPort(), appendMessage.getNonce());
            newValue.setClientSignature(appendMessage.getSignature());
            leaderState.setCurrent(newValue);
        }

        // sends a READ message to all members to collect their states
        ReadMessage readMessage = new ReadMessage(leaderState.getInstance());
        dcLogger.log("Broadcasting: " + readMessage);
        broadCastMessage(readMessage);
        dcLogger.log("Waiting for state quorum of size: " + config.getByzantineQuorum() + "...");
        List<StateMessage> states = leaderState.waitForStateQuorum();
        if (leaderState.getCaughtInvalidSignature()) {
            dcLogger.log("Caught invalid signature in state quorum");
            this.caughtInvalidSignature = true;
        }
        dcLogger.log("Quorum of STATE reached");

        // Send the collection of states to all the members
        CollectedMessage collectedMessage = new CollectedMessage(states, config.getPort(), leaderState.getInstance());
        dcLogger.log("Broadcasting: " + collectedMessage);
        broadCastMessage(collectedMessage);
    }

    /***
     * Chooses a value from collection of states and begins write phase
     * @return false if it aborted, else true
     */
    public boolean decideValue() {
        dcLogger.log("Waiting for states to decide value");
        List<StateMessage> collectedStates = this.consensusState.waitForCollectedMessage();
        if (collectedStates == null || collectedStates.size() < config.getByzantineQuorum()) {
            // abort
            dcLogger.log("aborted after collecting states");
            this.consensusState.nextEpoch();
            return false;
        }
        ValueTimestampPair decidedVTP = decideOnCollectedValues(collectedStates);
        if (decidedVTP == null) {
            // abort
            dcLogger.log("aborted after deciding null value");
            this.consensusState.nextEpoch();
            return false;
        }
        //ValueTimestampPair decidePair = new ValueTimestampPair(this.consensusState.getEpoch(), value);
        this.consensusState.updateWriteSet(decidedVTP);
        WriteMessage writeMessage = new WriteMessage(decidedVTP, config.getPort(), consensusState.getInstance());
        dcLogger.log("Broadcasting: " + writeMessage);
        broadCastMessage(writeMessage);
        return writePhase();
    }

    /***
     * Write phase of Consensus algorithm
     * @return false if aborted, true if a value was DECIDED and appended to blockchain
     */
    public boolean writePhase() {
        dcLogger.log("Waiting for write quorum of size: " + config.getByzantineQuorum() + "...");
        ValueTimestampPair writeValts = this.consensusState.waitForWriteQuorum(config.getByzantineQuorum());
        if (writeValts == null) {
            // Abort
            dcLogger.log("ABORTED (WRITE)");
            this.consensusState.nextEpoch();
            return false;
        }
        dcLogger.log("Quorum of WRITE reached");
        //ValueTimestampPair writeValts = new ValueTimestampPair(this.consensusState.getEpoch(), writeValue);
        this.consensusState.setCurrent(writeValts);

        // Broadcast ACCEPT and wait for quorum to DECIDE value
        AcceptMessage acceptMessage = new AcceptMessage(consensusState.getCurrent(), config.getPort(), consensusState.getInstance());
        dcLogger.log("Broadcasting: " + acceptMessage);
        broadCastMessage(acceptMessage);

        dcLogger.log("Waiting for accept quorum of size: " + config.getByzantineQuorum() + "...");
        ValueTimestampPair accept = this.consensusState.waitForAcceptQuorum(config.getByzantineQuorum());
        if (accept == null) {
            // Abort
            dcLogger.log("ABORTED (ACCEPT)");
            this.consensusState.nextEpoch();
            return false;
        }
        dcLogger.log("Quorum of accept reached");

        // DECIDE value
        this.stringChain.appendString(consensusState.getCurrent().getValue());
        ClientReplyMessage clientReplyMessage = new ClientReplyMessage(accept.getValue(), true, consensusState.getInstance());
        sendToClient(clientReplyMessage, accept.getClientPort());
        //broadCastToClients(clientReplyMessage);
        this.consensusState.nextInstance();
        return true;
    }


    /***
     * Decide on a value to write based on the collection of
     * States received from the other processes
     * @param collectedStates collection of all the members states
     * @return the value to write during this epoch, null if did not find any
     */
    public ValueTimestampPair decideOnCollectedValues(List<StateMessage> collectedStates) {
        ValueTimestampPair leaderValue = null;
        ValueTimestampPair highest = new ValueTimestampPair(-1, null, -1, -1);

        for (StateMessage thisState : collectedStates) {
            if (!verifyMemberStateAuthenticity(thisState)) {
                dcLogger.error("Signature is invalid for " + thisState.getState().getMemberName());
                // TODO -> hardcoded for the test (fix)
                this.caughtInvalidSignature = true;
                continue;
            } else {
                dcLogger.log("Signature is valid for " + thisState.getState().getMemberName());
            }
            if (thisState.getConsensusInstance() != this.consensusState.getInstance()) {
                dcLogger.error("State message of different instance among COLLECTED");
                continue;
            }

            ValueTimestampPair vts = thisState.getState().getCurrent();
            // sets vts clientName to name that comes in the consensus state inside state message
            //vts.setClientName(thisState.getState().getMemberName());
            if (highest.getTimestamp() > vts.getTimestamp()) {
                continue;
            }
            if (thisState.getState().getMemberName().equalsIgnoreCase(config.getLeader().getEntityName())) {
                leaderValue = vts;
            }

            // count in how many writesets it appears
            // must be at least 2f +1
            int count = 0;
            for (StateMessage otherState : collectedStates) {
                for (ValueTimestampPair pair : otherState.getState().getWriteset()) {
                    if (pair.getValue().equals(vts.getValue()) && pair.getTimestamp() >= vts.getTimestamp()) {
                        count++;
                        break;
                    }
                }
                if (count >= config.getByzantineQuorum()) {
                    highest = vts;
                    break;
                }
            }
        }

        if (highest.getValue() != null) {
            return highest;
        }
        // default to leader value
        if (leaderValue == null || leaderValue.getValue() == null) {
            dcLogger.error("Did not get leader value in COLLECTED message");
            return null;
        }
        if (!checkClientSignature(leaderValue)) {
            dcLogger.error("Leader forged new value");
            return null;
        }
        return leaderValue;
    }

    private void checkBalance(TransferMessage msg) {
    }

    public boolean verifyMemberStateAuthenticity(StateMessage stateMessage) {
        ConsensusState consensusSt = stateMessage.getState();
        String memberName = consensusSt.getMemberName();
        PublicKey memberPubKey = Security.getMembershipPublicKey(memberName);
        String reconstructSignatureData = consensusSt.getCurrent().toString() + consensusSt.getWriteset().toString() + consensusSt.getInstance() + consensusSt.getEpoch();
        return Security.verifyDS(stateMessage.getSignature(), reconstructSignatureData, memberPubKey);
    }

    public boolean checkClientSignature(ValueTimestampPair leaderVts) {
        String clientSignature = leaderVts.getClientSignature();
        if (clientSignature == null) return false;
        // TODO -> fix this hardcoded for 1 client (FIX GETFIRST())
        dcLogger.log("Leader Vts: " + leaderVts);
        String reconstructSignatureData = leaderVts.getValue() + leaderVts.getNonce();
        PublicKey clientPubKey = Security.getMembershipPublicKey(config.getClients().getFirst().getEntityName());
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
        dcLogger.log("Sending " + message.getType() + " message... to client " + message.getPort());
        perfectLink.sendMessage(message, port);
    }

    public void broadCastToClients(Message message) {
        dcLogger.log("BroadCasting to clients " + message.getType() + " message...");
        for (Entity client : config.getClients()) {
            dcLogger.log("[" + message.getType() + " MESSAGE]: " + client.getEntityName());
            perfectLink.sendMessage(message, client.getPort());
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

    public StringChain getBlockchainState() {
        return stringChain;
    }

    public void stop() {
        perfectLink.stop();
        running = false;
    }

    public boolean caughtInvalidSignature() {
        return this.caughtInvalidSignature;
    }

    public String getName() {
        return config.getMyName();
    }
}