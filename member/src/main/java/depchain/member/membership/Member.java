package depchain.member.membership;

import depchain.common.SignatureUtils;

import java.security.KeyPair;
import java.util.List;

public class Member {

private KeyPair keyPair;
	private List<MemberData> members;
	private String memberName;

	public Member(String memberName, List<MemberData> members) {
		this.memberName = memberName;
		this.members = members;
		readMyKeyPair(memberName);
	}

	public void readMyKeyPair(String memberName) {
		this.keyPair = SignatureUtils.getMemberKeyPair(memberName);
	}

	public boolean isLeader() {
		MemberData leader = MembershipManager.getLeader();
		if (leader == null) {
			return false;
		}
		return leader.getMemberName().equalsIgnoreCase(memberName);
	}

	public KeyPair getKeyPair() {
		return keyPair;
	}

	public List<MemberData> getMembers() {
		return members;
	}

	public String getMemberName() {
		return memberName;
	}

}