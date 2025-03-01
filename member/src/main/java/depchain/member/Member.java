package depchain.member;

import java.security.KeyPair;
import java.util.ArrayList;

import static depchain.library.SignatureUtils.getMemberKeyPair;

public class Member {

	private KeyPair keyPair;

	private ArrayList<MemberData> members;

	public Member(String memberName, ArrayList<MemberData> members) {
		this.members = members;
		readMyKeyPair(memberName);
	}

	public void readMyKeyPair(String memberName) {
		this.keyPair = getMemberKeyPair(memberName);
	}
}
