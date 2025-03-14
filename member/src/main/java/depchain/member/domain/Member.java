package depchain.member.domain;

import depchain.common.*;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.*;

import depchain.member.state.BlockchainState;
import java.net.DatagramSocket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Member {
	private String baseDir = System.getProperty("user.dir");
	private static final String LEADER_FILE = "/membership/leader.txt";
	protected final List<Entity> members;
	private final List<Entity> clients;
	protected Entity leader;
	protected final String myName;
	protected final int port;
	protected PerfectLink perfectLink;
	protected final DCLogger dcLogger;
	protected ConsensusState consensusState;
	protected final BlockchainState blockchainState;
	private final int faultyProcesses;
	protected final int byzantineQuorum;
	private final String address;
	private final boolean debug;
	protected BlockingQueue<Message> messageQueue;
	protected BlockingQueue<AppendMessage> appendQueue;
	private volatile boolean running = true;
	private boolean caughtInvalidSignature = false;

	public Member(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
		this.myName = memberName;
		this.members = members;
		this.clients = clients;
		this.port = port;
		this.address = address;
		this.debug = false;
		this.baseDir = System.getProperty("user.dir");
		this.dcLogger = new DCLogger(Member.class, debug, baseDir + "/logs/test/member-" + memberName + ".log");
		this.faultyProcesses = Math.floorDiv(members.size() - 1, 3);
		this.byzantineQuorum = members.size() - faultyProcesses;
		this.blockchainState = new BlockchainState(new ArrayList<>());
		this.messageQueue = new LinkedBlockingQueue<>();
		this.appendQueue = new LinkedBlockingQueue<>();
	}

    public void start() throws Exception {
		this.leader = CommonUtils.getLeader(baseDir + LEADER_FILE);
		if (this.leader == null) {
			dcLogger.error("Leader not found");
			return;
		}
		dcLogger.log("Byzantine quorum: " + byzantineQuorum);
		startConnections();
		new Thread(this::doConsensus).start();
		receiveMessages();
    }

	/***
	 * Establishes encrypted sessions with other members
	 * @throws Exception
	 */
	public void startConnections() throws Exception {
		// init consensus state
		dcLogger.log("Faulty processes ceiling (f): " + faultyProcesses);
		if (isLeader()) {
			this.consensusState = new ConsensusLeaderState(myName, 0);
		} else {
			this.consensusState = new ConsensusState(myName, 0);
		}
		dcLogger.log("Clients: " + this.clients);
		DatagramSocket serverSocket = new DatagramSocket(port);
		List<Entity> entities = new ArrayList<>();
		entities.addAll(clients);
		entities.addAll(members);
		KeyPair myKeyPair = Security.getMemberKeyPair(baseDir, myName);
		if (myKeyPair == null) {
			dcLogger.error("Keys not loaded successfully.");
		}
		// perfect link starts listening for messages
		createPerfectLink(serverSocket, myKeyPair, entities, false);
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
					default:
						dcLogger.log("Unknown message type");
				}
			} catch (InterruptedException e) {
				dcLogger.error("Error while processing message: " + e.getMessage());
			}
		}
	}

	public void handleRead(ReadMessage readMessage) {
		dcLogger.log("Received: " + readMessage);
		// sign: current||writeset||instance||epoch
		String dataToSign = consensusState.getCurrent().toString() + consensusState.getWriteset() + consensusState.getInstance() + consensusState.getEpoch();
		dcLogger.log("Signing data: " + dataToSign);
		String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(myName));
		// don't send own instance of consensus state to message, send a copy or else stack overflow error
		ConsensusState myState = new ConsensusState(myName, consensusState.getCurrent(), consensusState.getWriteset());
		// TODO -> i used the setter here to not change the constructor, use the constructor later
		myState.setInstance(consensusState.getInstance());
		StateMessage stateMessage = new StateMessage(myState, mySignature, consensusState.getInstance(), this.port);
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
		if (collectedMessage.getPort() != leader.getPort()) {
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


	/***
	 * Waits for new append requests and starts consensus to add them in the blockchain
	 */
	public void doConsensus(){
		while (running) {
			if (leader.getPort() != this.port) {
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
		dcLogger.log("Received: " + appendMessage);
		dcLogger.log("-- STARTING CONSENSUS FOR '" + appendMessage.getValue() + "' --");

		// if I am the leader and this is the first epoch I should propose the value
		// of the message
		ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
		if (consensusState.getCurrent().getValue().isEmpty()) {
			ValueTimestampPair newValue = new ValueTimestampPair(0, appendMessage.getValue());
			newValue.setClientSignature(appendMessage.getSignature());
			leaderState.setCurrent(newValue);
		}

		// sends a READ message to all members to collect their states
		ReadMessage readMessage = new ReadMessage(leaderState.getInstance());
		dcLogger.log("Broadcasting: " + readMessage);
		broadCastMessage(readMessage);
		dcLogger.log("Waiting for state quorum of size: " + byzantineQuorum + "...");
		List<StateMessage> states = leaderState.waitForStateQuorum();
		if (leaderState.getCaughtInvalidSignature()) {
			dcLogger.log("Caught invalid signature in state quorum");
			this.caughtInvalidSignature = true;
		}
		dcLogger.log("Quorum of STATE reached");

		// Send the collection of states to all the members
		CollectedMessage collectedMessage = new CollectedMessage(states, this.port, leaderState.getInstance());
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
		if (collectedStates == null || collectedStates.size() < this.byzantineQuorum) {
			// abort
			dcLogger.log("aborted after collecting states");
			this.consensusState.nextEpoch();
			return false;
		}
		String value = decideOnCollectedValues(collectedStates);
		if (value == null) {
			// abort
			dcLogger.log("aborted after deciding null value");
			this.consensusState.nextEpoch();
			return false;
		}
		ValueTimestampPair decidePair = new ValueTimestampPair(this.consensusState.getEpoch(), value);
		this.consensusState.updateWriteSet(decidePair);
		WriteMessage writeMessage = new WriteMessage(decidePair, this.port, consensusState.getInstance());
		dcLogger.log("Broadcasting: " + writeMessage);
		broadCastMessage(writeMessage);
		return writePhase();
	}

	/***
	 * Write phase of Consensus algorithm
	 * @return false if aborted, true if a value was DECIDED and appended to blockchain
	 */
	public boolean writePhase() {
		dcLogger.log("Waiting for write quorum of size: " + byzantineQuorum + "...");
		String writeValue = this.consensusState.waitForWriteQuorum(this.byzantineQuorum);
		if (writeValue == null) {
			// Abort
			dcLogger.log("ABORTED (WRITE)");
			this.consensusState.nextEpoch();
			return false;
		}
		dcLogger.log("Quorum of WRITE reached");
		ValueTimestampPair writeValts = new ValueTimestampPair(this.consensusState.getEpoch(), writeValue);
		this.consensusState.setCurrent(writeValts);

		// Broadcast ACCEPT and wait for quorum to DECIDE value
		AcceptMessage acceptMessage = new AcceptMessage(consensusState.getCurrent().getValue(), this.port, consensusState.getInstance());
		dcLogger.log("Broadcasting: " + acceptMessage);
		broadCastMessage(acceptMessage);

		dcLogger.log("Waiting for accept quorum of size: " + byzantineQuorum + "...");
		String accept = this.consensusState.waitForAcceptQuorum(this.byzantineQuorum);
		if (accept == null) {
			// Abort
			dcLogger.log("ABORTED (ACCEPT)");
			this.consensusState.nextEpoch();
			return false;
		}
		dcLogger.log("Quorum of accept reached");

		// DECIDE value
		this.blockchainState.appendString(consensusState.getCurrent().getValue());
		if (isLeader()) {
			ClientReplyMessage clientReplyMessage = new ClientReplyMessage(accept,true, consensusState.getInstance());
			broadCastToClients(clientReplyMessage);
		}
		this.consensusState.nextInstance();
		return true;
	}


	/***
	 * Decide on a value to write based on the collection of
	 * States received from the other processes
	 * @param collectedStates collection of all the members states
	 * @return the value to write during this epoch, null if did not find any
	 */
	public String decideOnCollectedValues(List<StateMessage> collectedStates) {
		ValueTimestampPair leaderValue = null;
		ValueTimestampPair highest = new ValueTimestampPair(-1, null);

		for (StateMessage thisState : collectedStates) {
			if (!verifyMemberStateAuthenticity(thisState)) {
				dcLogger.error("Signature is invalid for " + thisState.getState().getMemberName());
				//ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
				//leaderState.setCaughtInvalidSignature(true);
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
			if (highest.getTimestamp() > vts.getTimestamp() ) {
				continue;
			}
			if (thisState.getState().getMemberName().equalsIgnoreCase(leader.getEntityName())) {
				leaderValue = vts;
			}

			// count in how many writesets it appears
			// must be at least 2f +1
			int count = 0;
			for (StateMessage otherState : collectedStates) {
				for (ValueTimestampPair pair : otherState.getState().getWriteset()) {
					if (pair.getValue().equals(vts.getValue()) && pair.getTimestamp() >= vts.getTimestamp()) {
						count ++;
						break;
					}
				}
				if (count >= this.byzantineQuorum) {
					highest = vts;
					break;
				}
			}
		}

		if (highest.getValue() != null) {
			return highest.getValue();
		}
		// default to leader value
		if (leaderValue == null || leaderValue.getValue() == null ) {
			dcLogger.error("Did not get leader value in COLLECTED message");
			return null;
		}
		if (!checkClientSignature(leaderValue)) {
			dcLogger.error("Leader forged new value");
			return null;
		}
		return leaderValue.getValue();
	}

	public boolean verifyMemberStateAuthenticity(StateMessage stateMessage) {
		ConsensusState consensusSt = stateMessage.getState();
		String memberName = consensusSt.getMemberName();
		PublicKey memberPubKey = Security.getMemberPublicKey(memberName);
		String reconstructSignatureData = consensusSt.getCurrent().toString() + consensusSt.getWriteset().toString() + consensusSt.getInstance() + consensusSt.getEpoch();
		//dcLogger.log("Reconstructing: " + reconstructSignatureData);
		return Security.verifyDS(stateMessage.getSignature(), reconstructSignatureData, memberPubKey);
	}

	public boolean checkClientSignature(ValueTimestampPair leaderVts) {
		String clientSignature = leaderVts.getClientSignature();
		if (clientSignature == null) return false;
		// TODO -> fix this (kind of hardcoded)
		PublicKey clientPubKey = Security.getMemberPublicKey(clients.get(0).getEntityName());
		return Security.verifyDS(clientSignature, leaderVts.getValue(), clientPubKey);
	}

	public void sendToLeader(Message message) {
		dcLogger.log("Sending to leader " + message.getType() + " message...");
		if (this.port != leader.getPort())
			perfectLink.sendMessage(message, leader.getPort());
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
		if (this.port != port) {
			perfectLink.sendMessage(message, port);
		}
		else {
			sendToMe(message);
		}
	}

	public void broadCastMessage(Message message) {
		for (Entity member : members) {
			if (member.getPort() == this.port) {
				sendToMe(message);
				continue;
			}
			perfectLink.sendMessage(message, member.getPort());
		}
	}

	public void broadCastToClients(Message message) {
		dcLogger.log("BroadCasting to clients " + message.getType() + " message...");
		for (Entity client : clients) {
			dcLogger.log("[" + message.getType() + " MESSAGE]: " + client.getEntityName());
			perfectLink.sendMessage(message, client.getPort());
		}
	}

	public boolean isInitializer(int port) {
		return port > this.port;
	}

	public void startSessionsWithOtherMembers() {
		for (Entity otherMember : this.members) {
			if (isInitializer(otherMember.getPort())) {
				dcLogger.log("Starting session with " + otherMember.getEntityName());
				perfectLink.startSession(otherMember.getPort());
			}
		}
	}

	public boolean isLeader() {
		if (this.leader == null) {
			return false;
		}
		return this.leader.getEntityName().equalsIgnoreCase(myName);
	}

	public BlockchainState getBlockchainState() {
		return blockchainState;
	}

	public void stop() {
		perfectLink.stop();
		running = false;
	}

	public ConsensusLeaderState getConsensusLeaderState() {
		if (!isLeader()) {
			dcLogger.error("Not the leader");
			return null;
		}
		return (ConsensusLeaderState) consensusState;
	}

	public boolean caughtInvalidSignature() {
		return this.caughtInvalidSignature;
	}

	public void createPerfectLink(DatagramSocket serverSocket, KeyPair myKeyPair, List<Entity> entities, boolean debug) {
		this.perfectLink = new PerfectLink(serverSocket, messageQueue, myKeyPair, entities, debug);
	}

	public String getName() {
		return myName;
	}
}