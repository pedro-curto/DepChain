package depchain.member.membership;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.PublicKey;

public class MemberData {

	private String memberName;

	private PublicKey publicKey;

	private SecretKey symKey = null;

	private String address;

	private int port;

	public MemberData(String memberName, PublicKey publicKey, String address, int port) {
		this.publicKey = publicKey;
		this.memberName = memberName;
		this.port = port;
	}

	public void setSymKey(SecretKey key) {
		this.symKey = key;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public int getPort() {
		return port;
	}

	public String getMemberName() {
		return memberName;
	}

	public SecretKey getSymKey() {
		return symKey;
	}

	public String getAddress() {
		return address;
	}
}
