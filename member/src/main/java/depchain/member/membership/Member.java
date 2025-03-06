package depchain.member.membership;

import depchain.common.Security;
import depchain.member.links.PerfectLink;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.List;

public class Member {

	private KeyPair keyPair;
	private List<MemberData> members;
	private String myName;
	private int port;

	public Member(String memberName, List<MemberData> members) {
		this.myName = memberName;
		this.members = members;
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

	public void generateMembersSecretKeys() {
		for (MemberData memberData : this.members) {
			if (isInitializer(memberData.getPort())) {
				// I'm responsible for generating the communication key
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

}