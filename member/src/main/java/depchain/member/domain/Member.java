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
	private List<Entity> members;
	private List<Entity> clients;
	private Entity leader;
	private String myName;
	private int port;
	private final String address;
	private final boolean debug;
	private PerfectLink perfectLink;
	private DCLogger dcLogger;
	private ConsensusState consensusState;
	private BlockchainState blockchainState;
	private final int faultyProcesses;
	private final int byzantineQuorum;
	private List<ConsensusState> memberStates;

	public Member(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
		this.myName = memberName;
		this.members = members;
		this.clients = clients;
		this.port = port;
		this.address = address;
		this.debug = debug;
		this.dcLogger = new DCLogger(Member.class, debug);
		this.leader = CommonUtils.getLeader(LEADER_FILE);
		// floor of (n-1)/3
		this.faultyProcesses = Math.floorDiv(members.size() - 1, 3);
		// N - f
		this.byzantineQuorum = members.size() - faultyProcesses;
	}

	private boolean isLeader() {
		System.out.println("Leader: " + this.leader);
		if (this.leader == null) {
			System.out.println("No leader found");
			return false;
		}
		return this.leader.getEntityName().equalsIgnoreCase(myName);
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

    public void start() throws Exception {
		// inits consensus state
		dcLogger.log("Faulty processes ceiling (f): " + faultyProcesses);
		if (isLeader()) {
			dcLogger.log("I am the leader");
			this.consensusState = new ConsensusLeaderState(myName, 0);
		} else {
			dcLogger.log("I am not the leader");
			this.consensusState = new ConsensusState(myName, 0);
		}
		DatagramSocket serverSocket = new DatagramSocket(port);
		BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
		dcLogger.log("Clients: " + this.clients);

		// initializing blockchain and consensus state
		this.blockchainState = new BlockchainState(new ArrayList<>());
		//ConsensusState consensusState = new ConsensusState(myName);
		//RequestHandler requestHandler = new RequestHandler(blockchainState);

		// start sessions
		List<Entity> entities = new ArrayList<>();
		entities.addAll(clients);
		entities.addAll(members);
		KeyPair myKeyPair = Security.getMemberKeyPair(myName);
		this.perfectLink = new PerfectLink(serverSocket, messageQueue, myKeyPair, entities, true);
		perfectLink.start();

		// starts sessions for all processes with ports bigger than mine
		startSessionsWithOtherMembers();
		dcLogger.log("My sessions: " + perfectLink.getSessions());

		// start a thread (executor) to pop messages from the queue and process them
//		try (ExecutorService consensusExecutor = Executors.newSingleThreadExecutor()) {
//			consensusExecutor.submit(new ConsensusHandler(messageQueue, this, consensusState, blockchainState));
//		} catch (Exception e) {
//			dcLogger.error("Error while processing message: " + e.getMessage());
//		}

		// handle incoming messages
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

	private void handleAppend(AppendMessage appendMessage) {
		if (!isLeader()) {
			dcLogger.log("I'm not the leader. Skipping append message");
			return;
		}
		dcLogger.log("-- STARTING CONSENSUS FOR " + appendMessage.getValue() + " --");
		// if I'm the leader, my consensusState is a ConsensusLeaderState
		ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
		// set the leader's current value to the append message
		// TODO -> quando é que damos append do valts ao writeset? só no write?
		ValueTimestampPair currentValts = consensusState.getCurrent();
		int newTs = currentValts.getTimestamp() + 1;
		ValueTimestampPair newValue = new ValueTimestampPair(newTs, appendMessage.getValue());
		leaderState.setCurrent(newValue);

		// sends a READ message to all members to collect their states
		for (Entity member : members) {
			if (member.getPort() == port) {
				// skip myself
				dcLogger.log("Skipping ReadMessage to myself at port " + port);
				continue;
			}
			dcLogger.log("Sending read message to " + member.getPort());
			ReadMessage readMessage = new ReadMessage();
			perfectLink.sendMessage(readMessage, member.getPort());
		}
		// at this point, we must wait for at least N -f STATE messages
		// we spawn a new thread to wait for the quorum and the main thread processes incoming STATE messages
		// TODO -> do we count our own state message as a leader for the N - f quorum, making it N - f - 1?
		new Thread(() -> {
			dcLogger.log("Waiting for quorum of size " + byzantineQuorum);
			// wait for N - f
			leaderState.waitForQuorum(this.byzantineQuorum);
			dcLogger.log("Quorum of STATE reached");
			// after we receive N - f STATE messages, we send a COLLECTED message to all members
			List<StateMessage> states = leaderState.getMemberStates();
			// append our own state -> (also, if we use the leader's consensus state, we create a recursive loop, don't)
			String dataToSign = this.consensusState.getCurrent().toString() + this.consensusState.getWriteset();
			String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(myName));
			ConsensusState myState = new ConsensusState(myName, newValue, consensusState.getWriteset());
			StateMessage myStateMsg = new StateMessage(myState, mySignature);
			states.add(myStateMsg);
			CollectedMessage collectedMessage = new CollectedMessage(states);
			dcLogger.log("Sending collected message: " + collectedMessage);
			for (Entity member : members) {
				if (member.getPort() == port) {
					// skip myself
					dcLogger.log("Skipping CollectedMessage to myself at port " + port);
					continue;
				}
				perfectLink.sendMessage(collectedMessage, member.getPort());
			}
			proceedToWritePhase(newValue);
		}).start();
	}

	private void handleRead(ReadMessage readMessage) {
		dcLogger.log("Received read message: " + readMessage);
		// send state message back
		// TODO -> assino o quê? o consensusState? fazemo
		String dataToSign = consensusState.getCurrent().toString() + consensusState.getWriteset();
		String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(myName));
		StateMessage stateMessage = new StateMessage(consensusState, mySignature);
		dcLogger.log("Sending state message: " + stateMessage);
		perfectLink.sendMessage(stateMessage, leader.getPort());
	}

	private void handleAccept(AcceptMessage acceptMessage) {
		dcLogger.log("Received accept message: " + acceptMessage);
		consensusState.addAcceptMessage(acceptMessage);
	}

	private void handleWrite(WriteMessage writeMessage) {
		dcLogger.log("Received write message: " + writeMessage);
		// vtp is initialized to null, sanity check it first
		ValueTimestampPair vtp = consensusState.getCurrent();
		// TODO -> only add the write to the set of writes if it matches our currently decided value (?)
		if (vtp != null && writeMessage.getValts().getValue().equals(consensusState.getCurrent().getValue())) {
			consensusState.addWriteMessage(writeMessage);
		}

	}


	/*
	 * Members receive COLLECTED from the leader and check if there was a previously epoch decided value
	 * If not, they adopt the leader's value and send it in their WriteMessage
	 */
	private void handleCollected(CollectedMessage collectedMessage) {
		dcLogger.log("Received collected message: " + collectedMessage);
		ValueTimestampPair leaderValts = null;
		ValueTimestampPair highest = new ValueTimestampPair(-1, "");
		// for each message, check if the signature is valid
		// the conditions to decide on a value that isn't the leader, according to the lectures, are:
		// 1. collect the value that corresponds to the highest timestamp across all state messages
		for (StateMessage stateMessage : collectedMessage.getStates()) {
			ConsensusState consensusSt = stateMessage.getState();
			String memberName = consensusSt.getMemberName();
			PublicKey memberPubKey = Security.getMemberPublicKey(memberName);
			String reconstructSignatureData = consensusSt.getCurrent().toString() + consensusSt.getWriteset().toString();
			if (Security.verifyDS(stateMessage.getSignature(), reconstructSignatureData, memberPubKey)) {
				dcLogger.log("Signature is valid for " + memberName);
			} else {
				dcLogger.error("Signature is invalid for " + memberName);
				// TODO -> what to do? simply skip this message?
				continue;
			}
			ValueTimestampPair current = consensusSt.getCurrent();
			if (current != null && current.getTimestamp() > highest.getTimestamp()) {
				highest = current;
			}
			// also, it's useful to store the leader's value in case we want to adopt it
			if (memberName.equalsIgnoreCase(leader.getEntityName())) {
				dcLogger.log("Found leader's value: " + current);
				leaderValts = current;
			}
		}
		// 2. the value with highest ts must appear in the writeset of at least f+1 processes
		int count = 0;
		for (StateMessage stateMessage : collectedMessage.getStates()) {
			ConsensusState consensusSt = stateMessage.getState();
			if (consensusSt.getWriteset().contains(highest)) {
				count++;
			}
		}
		dcLogger.log("Highest value: " + highest);
		ValueTimestampPair proposedValue;
		// TODO -> aqui tem de ser adicionado a escolha ao writeset
		if (count >= this.faultyProcesses+1) {
			// proposed value is highest found
			proposedValue = highest;
		} else {
			proposedValue = leaderValts;
		}
		dcLogger.log("Leader valts: " + leaderValts);
		dcLogger.log("Proposed value: " + proposedValue);
		proceedToWritePhase(proposedValue);
	}

	private void handleState(StateMessage stateMessage) {
		if (isLeader()) {
			// leader: append the state to my consensus state
			ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
			leaderState.addMemberState(stateMessage);
			dcLogger.log("Received state message: " + stateMessage + ". Appended to leader state");
		} else {
			dcLogger.log("Received state message but not the leader");
		}


	}

	private void proceedToWritePhase(ValueTimestampPair proposedValue) {
		// at this point, I'm going to broadcast the proposed value in a WRITE message
		// if I do so, I need to append it to my state's writeset
		this.consensusState.addWritesetEntry(proposedValue);
		this.consensusState.setCurrent(proposedValue);
		WriteMessage writeMessage = new WriteMessage(proposedValue);
		// TODO -> add write message to our own quorum: isto está certo?
		consensusState.addWriteMessage(writeMessage);
		dcLogger.log("Sending write message: " + writeMessage);
		for (Entity member : members) {
			if (member.getPort() == port) {
				// skip myself
				dcLogger.log("Skipping WriteMessage to myself at port " + port);
				continue;
			}
			perfectLink.sendMessage(writeMessage, member.getPort());
		}
		// after sending the write message, we wait for a byzantine quorum of WRITE messages
		new Thread(() -> {
			dcLogger.log("Waiting for quorum of size " + byzantineQuorum);
			// TODO -> do we count our own write message for the N - f quorum?
			List<WriteMessage> writes = this.consensusState.waitForWriteQuorum(this.byzantineQuorum);
			dcLogger.log("Quorum of WRITE reached");
			// add current value to writeset
			consensusState.addWritesetEntry(consensusState.getCurrent());
			// after we receive a quorum of WRITE messages, we send an ACCEPT message to all members
			AcceptMessage acceptMessage = new AcceptMessage(this.consensusState.getCurrent().getValue());
			// TODO -> add accept message to our own quorum: isto está certo?
			consensusState.addAcceptMessage(acceptMessage);
			dcLogger.log("Sending accept message: " + acceptMessage);
			for (Entity member : members) {
				if (member.getPort() == port) {
					// skip myself
					dcLogger.log("Skipping AcceptMessage to myself at port " + port);
					continue;
				}
				perfectLink.sendMessage(acceptMessage, member.getPort());
			}
			// wait for quorum of ACCEPT and either ep-decide or abort
			List<AcceptMessage> accepts = this.consensusState.waitForAcceptQuorum(this.byzantineQuorum);
			dcLogger.log("Quorum of ACCEPT reached");
			// TODO -> decide or abort
			this.blockchainState.appendString(consensusState.getCurrent().getValue());
			int instance = consensusState.getCurrentConsensusInstance();
			// TODO -> answer back to the client if I'm the leader: if it's successful, implement logic to return false
			if (isLeader()) {
				String value = consensusState.getCurrent().getValue();
				// if value is different from null, we decided; otherwise, we aborted
				boolean success = value != null;
				ClientReplyMessage clientReplyMessage = new ClientReplyMessage(value, success, instance);
				// TODO -> corrigir lógica para dar handle a mais que um cliente (?)
				perfectLink.sendMessage(clientReplyMessage, clients.get(0).getPort());
				this.consensusState = new ConsensusLeaderState(myName, instance + 1);
			} else {
				this.consensusState = new ConsensusState(myName, instance + 1);
			}
		}).start();
	}

}