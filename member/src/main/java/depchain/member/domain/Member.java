package depchain.member.domain;

import com.google.gson.Gson;
import depchain.common.*;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.messaging.*;

import depchain.member.state.BlockchainState;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Member {
	private static final String LEADER_FILE = "membership/leader.txt";
	private List<Entity> members;
	private List<Entity> clients;
	private int leaderPort;
	private String myName;
	private int port;
	private final String address;
	private PerfectLink perfectLink;
	private DCLogger dcLogger = new DCLogger(Member.class);
	private ConsensusState consensusState;
	private final int faultyProcesses;

	public Member(String memberName, List<Entity> members, List<Entity> clients, int port, String address) {
		this.myName = memberName;
		this.members = members;
		this.clients = clients;
		this.port = port;
		this.address = address;
		// floor of (n-1)/3
		this.faultyProcesses = Math.floorDiv(members.size() - 1, 3);
	}

	private boolean isLeader() {
		Entity leader = CommonUtils.getLeader(LEADER_FILE);
		System.out.println("Leader: " + leader);
		if (leader == null) {
			System.out.println("No leader found");
			return false;
		}
		return leader.getEntityName().equalsIgnoreCase(myName);
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
			this.consensusState = new ConsensusLeaderState(myName);
		} else {
			dcLogger.log("I am not the leader");
			this.consensusState = new ConsensusState(myName);
		}
		this.leaderPort = CommonUtils.getLeader(LEADER_FILE).getPort();
		DatagramSocket serverSocket = new DatagramSocket(port);
		BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
		dcLogger.log("Clients: " + this.clients);

		// initializing blockchain and consensus state
		BlockchainState blockchainState = new BlockchainState(new ArrayList<>());
		ConsensusState consensusState = new ConsensusState(myName);
		//RequestHandler requestHandler = new RequestHandler(blockchainState);

		// start sessions
		List<Entity> entities = new ArrayList<>();
		entities.addAll(clients);
		entities.addAll(members);
		KeyPair myKeyPair = Security.getMemberKeyPair(myName);
		this.perfectLink = new PerfectLink(serverSocket, messageQueue, myKeyPair, entities);
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
						// TODO -> tive de incluir este if, mas como é que o próprio leader sequer recebe esta mensagem?
						if (isLeader()) continue;
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
		// if I'm the leader, my consensusState is a ConsensusLeaderState
		ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
		// sends a READ message to all members to collect their states
		for (Entity member : members) {
			if (member.getPort() == port) {
				// skip myself
				dcLogger.log("Skipping ReadMessage to myself at port " + port);
				continue;
			}
			// TODO -> como é que o membro depois recebe a mensagem e contacta o líder?
			ReadMessage readMessage = new ReadMessage();
			perfectLink.sendMessage(readMessage, member.getPort());
		}
		// at this point, we must wait for at least N -f STATE messages
		// we spawn a new thread to wait for the quorum and the main thread processes incoming STATE messages
		new Thread(() -> {
			int quorumSize = members.size() - faultyProcesses;
			dcLogger.log("Waiting for quorum of size " + quorumSize);

			leaderState.waitForQuorum(members.size() - faultyProcesses);
			dcLogger.log("Quorum of STATE reached");
			// after we receive N - f STATE messages, we send a COLLECTED message to all members
			List<ConsensusState> states = leaderState.getMemberStates();
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
		}).start();
	}

	private void handleRead(ReadMessage readMessage) {
		dcLogger.log("Received read message: " + readMessage);
		// send state message back
		StateMessage stateMessage = new StateMessage(consensusState);
		dcLogger.log("Sending state message: " + stateMessage);
		perfectLink.sendMessage(stateMessage, leaderPort);
	}

	private void handleAccept(AcceptMessage acceptMessage) {
	}

	private void handleWrite(WriteMessage writeMessage) {

	}

	private void handleCollected(CollectedMessage collectedMessage) {
		dcLogger.log("Received collected message: " + collectedMessage);
	}

	private void handleState(StateMessage stateMessage) {
		if (isLeader()) {
			// leader: append the state to my consensus state
			ConsensusLeaderState leaderState = (ConsensusLeaderState) consensusState;
			leaderState.addMemberState(stateMessage.getState());
			dcLogger.log("Received state message: " + stateMessage + ". Appended to leader state");
		} else {
			dcLogger.log("Received state message but not the leader");
		}


	}

}