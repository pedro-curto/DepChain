package depchain.common.messaging.library;

import depchain.common.messaging.CoinType;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

import java.math.BigInteger;

public class ApproveMessage extends Message {
	private String owner;
	private String spender;
	private BigInteger value;
	private String signature;
	private long nonce;

	public ApproveMessage(String owner, String spender, BigInteger value, CoinType coinType, String signature, long nonce) {
		super(MessageType.APPROVE, coinType);
		this.owner = owner;
		this.spender = spender;
		this.value = value;
		this.signature = signature;
		this.nonce = nonce;
	}

	public String getOwner() {
		return owner;
	}

	public String getSpender() {
		return spender;
	}

	public BigInteger getValue() {
		return value;
	}

	public String getSignature() {
		return signature;
	}

	public long getNonce() {
		return nonce;
	}

	@Override
	public String toString() {
		return "ApproveMessage{" +
				"owner='" + owner + '\'' +
				", spender='" + spender + '\'' +
				", value=" + value +
				", nonce=" + nonce +
				'}';
	}
	
}
