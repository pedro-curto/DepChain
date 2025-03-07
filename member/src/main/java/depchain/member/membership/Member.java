package depchain.member.membership;

import depchain.common.DCLogger;
import depchain.common.messaging.Message;
import depchain.common.PerfectLink;
import depchain.common.Security;

import depchain.member.messaging.*;
import depchain.member.state.BlockchainState;
import depchain.member.state.RequestHandler;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Member {

	private KeyPair keyPair;
	private List<MemberData> members;
	private String myName;
	private int port;
	private String address;
	private PerfectLink perfectLink;
	private DCLogger dcLogger;

	public Member(String memberName, List<MemberData> members, int port, String address) {
		this.myName = memberName;
		this.members = members;
		this.port = port;
		this.address = address;
		readMyKeyPair(memberName);
	}

	public void readMyKeyPair(String memberName) {
		this.keyPair = Security.getMemberKeyPair(memberName);
	}

	public boolean isLeader() {
		MemberData leader = MembershipManager.getLeader();
		System.out.println("Leader: " + leader);
		if (leader == null) {
			System.out.println("No leader found");
			return false;
		}
		return leader.getMemberName().equalsIgnoreCase(myName);
	}

	public boolean isInitializer(int port) {
		return port > this.port;
	}

	private void startSessionsWithOtherMembers() {
		for (MemberData otherMember : this.members) {
			if (isInitializer(otherMember.getPort())) {
				dcLogger.log("Starting session with " + otherMember.getMemberName());
				perfectLink.startSession(
						otherMember.getAddress(),
						otherMember.getPort(),
						keyPair,
						otherMember.getPublicKey());
			}
		}
	}

    public void start() throws Exception {
		this.dcLogger = new DCLogger(Member.class);
		dcLogger.log("Am I leader? " + this.isLeader());
		DatagramSocket serverSocket = new DatagramSocket(port);
		BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

		// initializing blockchain
		RequestHandler requestHandler = new RequestHandler(new BlockchainState(new ArrayList<>()));

		// start sessions
		KeyPair myKeyPair = Security.getMemberKeyPair(myName);
		this.perfectLink = new PerfectLink(serverSocket, messageQueue, myKeyPair);
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