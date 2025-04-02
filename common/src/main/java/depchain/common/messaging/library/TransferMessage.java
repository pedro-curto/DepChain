package depchain.common.messaging.library;

import depchain.common.messaging.CoinType;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

import java.math.BigInteger;

public class TransferMessage extends Message {
	private String from; // remove
	private String to; // remove
	private BigInteger value; // remove
	private long nonce;
	private String signature;

	public TransferMessage(String from, String to, BigInteger value, CoinType coinType, long nonce) {
		super(MessageType.TRANSFER, coinType);
		this.from = from;
		this.to = to;
		this.value = value;
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

	public void setSignature (String signature) {
		this.signature = signature;
	}

	public String getDataToSign() {
		return from + to + value + nonce;
	}

	@Override
	public String toString() {
		return "TransferMessage{" +
				"from='" + from + '\'' +
				", to='" + to + '\'' +
				", value=" + value +
				", nonce=" + nonce +
				", coinType=" + super.getCoinType() +
				'}';
	}


}
