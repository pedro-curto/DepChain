package depchain.common.messaging;

import java.math.BigInteger;

public class TransferMessage extends Message {
	private String from;
	private String to;
	private BigInteger value;
	private String signature;
	private long nonce;

	public TransferMessage(String from, String to, BigInteger value, CoinType coinType, String signature, long nonce) {
		super(MessageType.TRANSFER, coinType);
		this.from = from;
		this.to = to;
		this.value = value;
		this.signature = signature;
		this.nonce = nonce;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
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
		return "TransferMessage{" +
				"from='" + from + '\'' +
				", to='" + to + '\'' +
				", value=" + value +
				", nonce=" + nonce +
				'}';
	}


}
