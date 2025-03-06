package depchain.member.membership;

import depchain.common.Message;
import depchain.common.PerfectLink;
import depchain.common.Security;
import depchain.member.state.BlockchainState;
import depchain.member.state.RequestHandler;

import javax.crypto.SecretKey;
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

	public KeyPair getKeyPair() {
		return keyPair;
	}

	public List<MemberData> getMembers() {
		return members;
	}

	public String getMemberName() {
		return myName;
	}

	public boolean isInitializer(int port) {
		return port > this.port;
	}

	private void generateMembersSecretKeys() {
		for (MemberData memberData : this.members) {
			if (isInitializer(memberData.getPort())) {
				// i'm responsible for generating the communication key
				SecretKey key = Security.generateSecretKey();
				if (key != null) {
					memberData.setSymKey(key);
				} else {
					System.err.println("Error generating secret key for " + memberData.getMemberName());
				}
				byte[] keyPacket = Security.encryptSymKeyWithAsymKey(key, memberData.getPublicKey());
				// TODO send private key packet to receiver member
			}
		}
	}

    public void start() throws Exception {
		System.out.println("Am I leader? " + this.isLeader());
		DatagramSocket serverSocket = new DatagramSocket(port);
		BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

		// generates symmetric keys for all processes with ports bigger than mine
		generateMembersSecretKeys();

		// initializing blockchain
		RequestHandler requestHandler = new RequestHandler(new BlockchainState(new ArrayList<>()));

		// sessions


		// starts message handler and perfect link
		depchain.common.PerfectLink perfectLink = new PerfectLink(serverSocket, messageQueue);
		perfectLink.start();

		while (true) {
			Message message = messageQueue.take();
		}
    }
}