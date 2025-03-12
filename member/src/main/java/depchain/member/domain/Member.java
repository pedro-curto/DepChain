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
	private static final String LEADER_FILE = "membership/leader.txt";
	private final List<Entity> members;
	private final List<Entity> clients;
	private final Entity leader;
	private final String myName;
	private final int port;
	private PerfectLink perfectLink;
	private final DCLogger dcLogger;
	private ConsensusState consensusState;
	private final BlockchainState blockchainState;
	private final int faultyProcesses;
	private final int byzantineQuorum;
	private List<ConsensusState> memberStates;
	private final String address;
	private final boolean debug;

	//TODO -> temporary
	private BlockingQueue<Message> messageQueue;

	public Member(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
		this.myName = memberName;
		this.members = members;
		this.clients = clients;
		this.port = port;
		this.address = address;
		this.debug = debug;
		this.dcLogger = new DCLogger(Member.class, debug);
		this.leader = CommonUtils.getLeader(LEADER_FILE);
		this.faultyProcesses = Math.floorDiv(members.size() - 1, 3);
		this.byzantineQuorum = members.size() - faultyProcesses;
		this.blockchainState = new BlockchainState(new ArrayList<>());
	}

    public void start() throws Exception {
		// init consensus state
		dcLogger.log("Faulty processes ceiling (f): " + faultyProcesses);
		if (isLeader()) {
			dcLogger.log("I am the leader");
			this.consensusState = new ConsensusLeaderState(myName, 0);
		} else {
			dcLogger.log("I am not the leader");
			this.consensusState = new ConsensusState(myName, 0);
		}
		DatagramSocket serverSocket = new DatagramSocket(port);
		this.messageQueue = new LinkedBlockingQueue<>();
		dcLogger.log("Clients: " + this.clients);

		// perfect link starts listening for messages
		List<Entity> entities = new ArrayList<>();
		entities.addAll(clients);
		entities.addAll(members);
		KeyPair myKeyPair = Security.getMemberKeyPair(myName);
		this.perfectLink = new PerfectLink(serverSocket, messageQueue, myKeyPair, entities, true);
		perfectLink.start();

		// begins encrypted sessions with all member processes running on higher ports
		// others should do the same for this process
		startSessionsWithOtherMembers();
		dcLogger.log("My sessions: " + perfectLink.getSessions());

		// handle messages delivered by the perfect link
		while (true) {
			try {
				Message message = messageQueue.take();
				dcLogger.log("Received message of type: " + message.getType());

				switch (message.getType()) {
					case APPEND:
						AppendMessage appendMessage = (AppendMessage) message;
						dcLogger.log("Received append message: " + appendMessage);
						handleAppend(appendMessage);
						break;
					case READ:
						ReadMessage readMessage = (ReadMessage) message;
						handleRead(readMessage);
						break;
					case STATE:
						StateMessage stateMessage = (StateMessage) message;
						handleState(stateMessage);
						break;
					case COLLECTED:
						CollectedMessage collectedMessage = (CollectedMessage) message;
						handleCollected(collectedMessage);
						break;
					case WRITE:
						WriteMessage writeMessage = (WriteMessage) message;
						handleWrite(writeMessage);
						break;
					case ACCEPT:
						AcceptMessage acceptMessage = (AcceptMessage) message;
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

	private void handleRead(ReadMessage readMessage) {
		dcLogger.log("Received read message: " + readMessage);
		String dataToSign = consensusState.getCurrent().toString() + consensusState.getWriteset();
		String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(myName));
		StateMessage stateMessage = new StateMessage(consensusState, mySignature);
		dcLogger.log("Sending state message: " + stateMessage);
		if (this.port != leader.getPort())
			perfectLink.sendMessage(stateMessage, leader.getPort());
		else {
			//TODO -> same thing, need to fix this later (dybizantino)
            try {
                this.messageQueue.put(stateMessage);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
	}

	private void handleAccept(AcceptMessage acceptMessage) {
		dcLogger.log("Received accept message: " + acceptMessage);
		consensusState.addAcceptMessage(acceptMessage);
	}

	private void handleWrite(WriteMessage writeMessage) {
		dcLogger.log("Received write message: " + writeMessage);
		// TODO: verify signature (byzantine can forge other members writes)
		// vtp is initialized to null, sanity check it first
		ValueTimestampPair vtp = consensusState.getCurrent();
		// TODO: why the second condition?
		if (vtp != null && writeMessage.getValts().getValue().equals(consensusState.getCurrent().getValue())) {
			consensusState.addWriteMessage(writeMessage);
		}
	}

	private void handleState(StateMessage stateMessage) {
		if (isLeader()) {
			// leader: append the state to my consensus state
			ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
			leaderState.addMemberState(stateMessage);
			dcLogger.log("Received state message: " + stateMessage + ". Appended to leader state");
		} else {
			dcLogger.log("[ERROR] Received state message but not the leader");
		}
	}

	private void handleAppend(AppendMessage appendMessage) {
		if (!isLeader()) {
			dcLogger.log("I'm not the leader. Skipping append message");
			return;
		}
		dcLogger.log("-- STARTING CONSENSUS FOR " + appendMessage.getValue() + " --");
		ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
		// if I am the leader and this is the first epoch I should propose the value
		// of the message
		if (consensusState.getEpoch() == 0) {
			ValueTimestampPair newValue = new ValueTimestampPair(0, appendMessage.getValue());
			leaderState.setCurrent(newValue);
		}
		Thread thread = new Thread(() -> startConsensus(leaderState));
		thread.start();
	}

	private void handleCollected(CollectedMessage collectedMessage) {
		dcLogger.log("Received collected message: " + collectedMessage);
		String value = decideOnCollectedValues(collectedMessage.getStates());
		dcLogger.log("Decided value: " + value);
		ValueTimestampPair decidePair = new ValueTimestampPair(this.consensusState.getEpoch(), value);
		WriteMessage writeMessage = new WriteMessage(decidePair, this.port);
		broadCastMessage(writeMessage);
		Thread thread = new Thread(() -> writePhase(decidePair));
		thread.start();
	}

	private void startConsensus(ConsensusLeaderState leaderState) {
		// sends a READ message to all members to collect their states
		broadCastMessage(new ReadMessage());

		dcLogger.log("Waiting for quorum of size (including myself)" + byzantineQuorum +1);
		List<StateMessage> states = leaderState.waitForQuorum(this.byzantineQuorum + 1);
		if (states == null) {
			//abort
			this.consensusState.nextEpoch();
			return;
		}
		dcLogger.log("Quorum of STATE reached");

		// Send the collection of states to all the members
		CollectedMessage collectedMessage = new CollectedMessage(states);
		dcLogger.log("Sending collected message: " + collectedMessage);
		broadCastMessage(collectedMessage);
	}

	private void writePhase(ValueTimestampPair decidedPair) {
		dcLogger.log("Waiting for quorum of size (consensus) (including myself): " + byzantineQuorum +1);
		ValueTimestampPair writeValue = this.consensusState.waitForWriteQuorum(this.byzantineQuorum + 1);
		if (writeValue == null) {
			// Abort
			this.consensusState.nextEpoch();
			return;
		}
		dcLogger.log("Quorum of WRITE reached");
		this.consensusState.setCurrent(decidedPair);

		// Broadcast ACCEPT and wait for quorum to DECIDE value
		broadCastMessage(new AcceptMessage(this.consensusState.getCurrent().getValue()));
		dcLogger.log("Waiting for quorum of size (consensus) (including myself): " + byzantineQuorum +1);
		String accept = this.consensusState.waitForAcceptQuorum(this.byzantineQuorum + 1);
		if (accept == null) {
			// Abort
			this.consensusState.nextEpoch();
			return;
		}
		dcLogger.log("Quorum of ACCEPT reached");

		// DECIDE value
		this.blockchainState.appendString(consensusState.getCurrent().getValue());
		if (isLeader()) {
			ClientReplyMessage clientReplyMessage = new ClientReplyMessage(decidedPair.getValue(),true, consensusState.getCurrentConsensusInstance());
			broadCastToClients(clientReplyMessage);
		}
		this.consensusState.nextInstance();
	}

	private boolean verifyMemberStateAuthenticity(StateMessage stateMessage) {
		ConsensusState consensusSt = stateMessage.getState();
		String memberName = consensusSt.getMemberName();
		PublicKey memberPubKey = Security.getMemberPublicKey(memberName);
		String reconstructSignatureData = consensusSt.getCurrent().toString() + consensusSt.getWriteset().toString();
		return Security.verifyDS(stateMessage.getSignature(), reconstructSignatureData, memberPubKey);
	}

	/***
	 * Decide on a value to write based on the collection of
	 * States received from the other processes
	 * @param collectedStates collection of all the members states
	 * @return the value to write during this epoch
	 */
	private String decideOnCollectedValues(List<StateMessage> collectedStates) {
		ValueTimestampPair leaderValts = null;
		ValueTimestampPair highest = new ValueTimestampPair(-1, "");

		for (StateMessage stateMessage : collectedStates) {
			// check signature
			if (!verifyMemberStateAuthenticity(stateMessage)) {
				dcLogger.error("Signature is invalid for " + stateMessage.getState().getMemberName());
				continue;
			}
			// 1. collect the value that corresponds to the highest timestamp across all state messages
			ValueTimestampPair current = stateMessage.getState().getCurrent();
			String memberName = stateMessage.getState().getMemberName();
			if (current != null && current.getTimestamp() > highest.getTimestamp()) {
				highest = current;
			}
			// store the leader's value in case we don't find a value
			if (memberName.equalsIgnoreCase(leader.getEntityName())) {
				dcLogger.log("Found leader's value: " + current);
				leaderValts = current;
			}
		}
		// 2. the value with highest ts must appear in the writeset of at least f+1 processes
		int count = 0;
		for (StateMessage stateMessage : collectedStates) {
			if (stateMessage.getState().getWriteset().contains(highest)) {
				count++;
			}
		}
		// if we don't find a value, we default to leaders proposal
        return count >= this.faultyProcesses + 1 ? highest.getValue() : leaderValts.getValue();
	}

	private void broadCastMessage(Message message) {
		dcLogger.log("BroadCasting " + message.getType() + " message...");
		for (Entity member : members) {
			//TODO -> fita cola eu sei... eu depois resolvo (dybizantino)
			if (member.getPort() == this.port) {
                try {
                    messageQueue.put(message);
					continue;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
			dcLogger.log("[" + message.getType() + " MESSAGE]: " + member.getEntityName());
			perfectLink.sendMessage(message, member.getPort());
		}
	}

	private void broadCastToClients(Message message) {
		dcLogger.log("BroadCasting to clients " + message.getType() + " message...");
		for (Entity client : clients) {
			dcLogger.log("[" + message.getType() + " MESSAGE]: " + client.getEntityName());
			perfectLink.sendMessage(message, client.getPort());
		}
	}

	private boolean isInitializer(int port) {
		return port > this.port;
	}

	private void startSessionsWithOtherMembers() {
		for (Entity otherMember : this.members) {
			if (isInitializer(otherMember.getPort())) {
				dcLogger.log("Starting session with " + otherMember.getEntityName());
				perfectLink.startSession(otherMember.getPort());
			}
		}
	}

	private boolean isLeader() {
		System.out.println("Leader: " + this.leader);
		if (this.leader == null) {
			System.out.println("No leader found");
			return false;
		}
		return this.leader.getEntityName().equalsIgnoreCase(myName);
	}
}