package depchain.common.messaging;

import depchain.common.domain.Transaction;

public class ClientReplyMessage extends Message {
	private String value = null;
	private Transaction transaction = null;
	private boolean success;
	private int instanceOfDecision;

	public ClientReplyMessage(String value, boolean success, int instanceOfDecision) {
		super(MessageType.CLIENT_REPLY);
		this.value = value;
		this.success = success;
		this.instanceOfDecision = instanceOfDecision;
	}

	public ClientReplyMessage(Transaction transaction, boolean success, int instanceOfDecision) {
		super(MessageType.CLIENT_REPLY);
		this.transaction = transaction;
		this.success = success;
		this.instanceOfDecision = instanceOfDecision;
	}

	public String getValue() {
		return value;
	}

	public boolean getSuccess() {
		return success;
	}

	public int getInstanceOfDecision() {
		return instanceOfDecision;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	@Override
	public String toString() {
		return "ClientReply{" +
				"value='" + value + '\'' +
				"success='" + success + '\'' +
				", instanceOfDecision='" + instanceOfDecision + '\'' +
				'}';
	}
}
