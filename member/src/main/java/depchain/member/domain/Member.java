package depchain.member.domain;

import depchain.common.*;
import depchain.common.domain.Entity;
import depchain.common.messaging.Message;

import depchain.member.messaging.*;
import depchain.member.state.BlockchainState;
import depchain.member.state.RequestHandler;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Member {
	private static final String LEADER_FILE = "membership/leader.txt";
	private static final String CLIENT_FILE = "membership/client.txt";
	private List<Entity> members;
	private List<Entity> clients;
	private String myName;
	private int port;
	private String address;
	private PerfectLink perfectLink;
	private DCLogger dcLogger;

	public Member(String memberName, List<Entity> members, List<Entity> clients, int port, String address) {
		this.myName = memberName;
		this.members = members;
		this.clients = clients;
		this.port = port;
		this.address = address;
	}

	public boolean isLeader() {
		Entity leader = CommonUtils.getLeader(LEADER_FILE);
		System.out.println("Leader: " + leader);
		if (leader == null) {
			System.out.println("No leader found");
			return false;
		}
		return leader.getEntityName().equalsIgnoreCase(myName);
	}

	public boolean isInitializer(int port) {
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
		this.dcLogger = new DCLogger(Member.class);
		dcLogger.log("Am I leader? " + this.isLeader());
		DatagramSocket serverSocket = new DatagramSocket(port);
		BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
		dcLogger.log("Clients: " + this.clients);

		// initializing blockchain
		RequestHandler requestHandler = new RequestHandler(new BlockchainState(new ArrayList<>()));

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

		while (true) {

			Message message = messageQueue.take();
			dcLogger.log("Received message of type: " + message.getType());

			switch (message.getType()) {
				case APPEND:
					// TODO
					// call init() or propose()?
					// then they send READ messages to all members
					break;
				case READ:
					ReadMessage readMessage = (ReadMessage) message;
					requestHandler.handleRead(readMessage);
					break;
				case STATE:
					StateMessage stateMessage = (StateMessage) message;
					requestHandler.handleState(stateMessage);
					break;
				case COLLECTED:
					CollectedMessage collectedMessage = (CollectedMessage) message;
					requestHandler.handleCollected(collectedMessage);
					break;
				case WRITE:
					WriteMessage writeMessage = (WriteMessage) message;
					requestHandler.handleWrite(writeMessage);
					break;
				case ACCEPT:
					AcceptMessage acceptMessage = (AcceptMessage) message;
					requestHandler.handleAccept(acceptMessage);
					break;
				default:
					dcLogger.log("Unknown message type");
			}
		}
    }

	public void init() {
		// TODO
		// do this in Member or ConsensusLeaderState?
	}

	public void propose() {
		// TODO
		// do this in Member or ConsensusLeaderState?
	}

	public void decide() {
		// TODO
		// do this in Member or ConsensusLeaderState?
	}
}