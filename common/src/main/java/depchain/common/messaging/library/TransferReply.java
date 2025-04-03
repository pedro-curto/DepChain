package depchain.common.messaging.library;

import depchain.common.messaging.ClientReplyMessage;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.MessageType;

import java.math.BigInteger;
import java.util.Objects;

public class TransferReply extends ClientReplyMessage {
    private BigInteger amount;
    private String senderAddr;
    private String spenderAddr;
    private String recipientAddr;

    public TransferReply(
            boolean success,
            int instanceOfDecision,
            BigInteger amount,
            String senderAddr,
            String spenderAddr,
            String recipienAddr,
            CoinType coinType,
            long nonce,
            int memberPort
    ) {
        super(MessageType.TRANSFER_REPLY,
                success,
                instanceOfDecision,
                coinType,
                nonce,
                memberPort
        );
        this.amount = amount;
        this.senderAddr = senderAddr;
        this.spenderAddr = spenderAddr;
        this.recipientAddr = recipienAddr;
    }

    public BigInteger getAmount() {
        return amount;
    }
    public String getSenderAddr() {
        return senderAddr;
    }
    public String getSpenderAddr() {
        return spenderAddr;
    }
    public String getRecipientAddr() {
        return recipientAddr;
    }

    // for hashmap in client
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        if (!(obj instanceof TransferReply)) return false;
        TransferReply other = (TransferReply) obj;
        return (
                other.getAmount().equals(amount) &&
                other.getSenderAddr().equals(senderAddr) &&
                other.getSpenderAddr().equals(spenderAddr) &&
                other.getRecipientAddr().equals(recipientAddr)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), amount, senderAddr, spenderAddr, recipientAddr);
    }
}
