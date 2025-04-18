package depchain.common.messaging.library;

import depchain.common.messaging.CoinType;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;
import depchain.common.messaging.TransactionType;

import java.math.BigInteger;

public class TransferMessage extends Message {
	private String from;
	private String spender; // necessary for TRANSFER_FROM transactions
	private String to;
	private BigInteger value;
	private long nonce;
	private String signature;
	private TransactionType transactionType;
	private int clientPort;

	public TransferMessage(
			String from,
			String spender,
			String to,
			BigInteger value,
			CoinType coinType,
			long nonce,
			TransactionType transactionType,
			int clientPort
	) {
		super(MessageType.TRANSFER, coinType);
		this.from = from;
		this.spender = spender;
		this.to = to;
		this.value = value;
		this.nonce = nonce;
		this.transactionType = transactionType;
		this.clientPort = clientPort;
	}

	public String getFrom() {
		return from;
	}

	public String getSpender() {
		return spender;
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

	public int getClientPort() {
		return clientPort;
	}

	public void setSignature (String signature) {
		this.signature = signature;
	}

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public String getDataToSign() {
		if (transactionType == TransactionType.TRANSFER_FROM) {
			return from + spender + to + value + nonce + transactionType + clientPort;
		}
		return from + to + value + nonce + transactionType + clientPort;
	}

	@Override
	public String toString() {
		return "TransferMessage{" +
				"from='" + from + '\'' +
				", spender=" + spender + '\'' +
				", to='" + to + '\'' +
				", value=" + value +
				", nonce=" + nonce +
				", coinType=" + super.getCoinType() +
				", transactionType=" + transactionType +
				", clientPort=" + clientPort +
				", port=" + super.getPort() +
				'}';
	}


}
