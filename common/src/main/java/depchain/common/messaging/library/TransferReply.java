package depchain.common.messaging.library;

import depchain.common.messaging.ClientReplyMessage;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.MessageType;
import depchain.common.messaging.TransactionType;

import java.math.BigInteger;
import java.util.Objects;

public class TransferReply extends ClientReplyMessage {
    private BigInteger amount;
    private String senderAddr;
    private String spenderAddr;
    private String recipientAddr;
    private TransactionType transactionType;

    public TransferReply(
            boolean success,
            int instanceOfDecision,
            BigInteger amount,
            String senderAddr,
            String spenderAddr,
            String recipienAddr,
            CoinType coinType,
            TransactionType transactionType,
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
        this.transactionType = transactionType;
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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public String toString() {
        return "TransferReply{" +
                "amount=" + amount +
                ", senderAddr='" + senderAddr + '\'' +
                ", spenderAddr='" + spenderAddr + '\'' +
                ", recipientAddr='" + recipientAddr + '\'' +
                ", transactionType=" + transactionType +
                ", success=" + super.getSuccess() +
                ", instanceOfDecision=" + super.getInstanceOfDecision() +
                ", coinType=" + coinType +
                ", nonce=" + super.getNonce() +
                ", memberPort=" + super.getPort() +
                '}';
    }

    // for hashmap in client
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof TransferReply)) return false;

        TransferReply other = (TransferReply) obj;

        return Objects.equals(this.amount, other.getAmount()) &&
                Objects.equals(this.senderAddr, other.getSenderAddr()) &&
                Objects.equals(this.spenderAddr, other.getSpenderAddr()) &&
                Objects.equals(this.recipientAddr, other.getRecipientAddr());
    }


    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), amount, senderAddr, spenderAddr, recipientAddr);
    }
}
