package depchain.common.messaging.library;

import depchain.common.messaging.ClientReplyMessage;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.MessageType;

import java.math.BigInteger;
import java.util.Objects;

public class BalanceReply extends ClientReplyMessage {
    private String address;
    private BigInteger balance;

    public BalanceReply(
            boolean success,
            int instanceOfDecision,
            String address,
            BigInteger balance,
            CoinType coinType,
            long nonce,
            int memberPort
    ) {
        super(MessageType.BALANCE_REPLY,
                success,
                instanceOfDecision,
                coinType,
                nonce,
                memberPort
        );
        this.address = address;
        this.balance = balance;
    }

    public String getAddress() {
        return address;
    }
    public BigInteger getBalance() {
        return balance;
    }

    public String getDataToSign() {
        return address + balance;
    }

    // for hashmap in client
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        if (!(obj instanceof BalanceReply)) return false;
        BalanceReply other = (BalanceReply) obj;
        return (
                other.getAddress().equals(address) &&
                other.getBalance().equals(balance)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), address, balance);
    }
}