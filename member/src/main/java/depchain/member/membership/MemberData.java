package depchain.member.membership;

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

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public String getMemberName() {
		return memberName;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return memberName + " " + address + " " + port;
	}

}
