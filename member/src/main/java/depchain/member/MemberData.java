package depchain.member;

import java.security.PublicKey;

public class MemberData {

	private PublicKey publicKey;

	private String memberName;

	private String address;

	private int port;

	public MemberData(String memberName, PublicKey publicKey, String address, int port) {
		this.publicKey = publicKey;
		this.memberName = memberName;
		this.address = address;
		this.port = port;
	}

}
