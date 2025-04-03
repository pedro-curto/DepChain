package depchain.common.messaging;

import depchain.common.domain.Transaction;

import java.util.Objects;

public class ClientReplyMessage extends Message {
    private String value = "";
    private boolean success;
    private int instanceOfDecision;
	MessageType replyType;
    private long nonce;

    public ClientReplyMessage(String value, boolean success, int instanceOfDecision, MessageType replyType) {
        super(replyType);
        this.value = value;
        this.success = success;
        this.instanceOfDecision = instanceOfDecision;
		this.replyType = replyType;
    }

	public ClientReplyMessage(
        MessageType replyType,
        boolean success,
        int instanceOfDecision,
        CoinType coinType,
        long nonce,
        int memberPort
    ) {
		super(replyType, memberPort);
		this.replyType = replyType;
		this.success = success;
		this.instanceOfDecision = instanceOfDecision;
        this.coinType = coinType;
        this.nonce = nonce;
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

    public MessageType getReplyType() {
        return replyType;
    }
    public long getNonce() {
        return nonce;
    }

    // for hashmap in client
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClientReplyMessage other) {
            return (
                    other.getSuccess() == success &&
                    other.getInstanceOfDecision() == instanceOfDecision &&
                    other.getValue().equals(value) &&
                    other.getCoinType().equals(coinType) &&
                    other.getNonce() == nonce
            );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value + success + instanceOfDecision + coinType + replyType + nonce);
    }

    @Override
    public String toString() {
        return "ClientReply{" +
                "value='" + value + '\'' +
                "success='" + success + '\'' +
                ", instanceOfDecision='" + instanceOfDecision + '\'' +
                ", replyType=" + replyType +
                ", nonce=" + nonce +
                ", coinType=" + coinType +
                '}';
    }
}
