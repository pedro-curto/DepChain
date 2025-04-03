package depchain.common.messaging.library;

import depchain.common.messaging.ClientReplyMessage;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.MessageType;

import java.math.BigInteger;
import java.util.Objects;

public class AllowanceReply extends ClientReplyMessage {

    private String owner;
    private String spender;
    private BigInteger allowance;

    public AllowanceReply(
            boolean success,
            int instanceOfDecision,
            String owner,
            String spender,
            BigInteger allowance,
            CoinType coinType,
            long nonce
    ) {
        super(MessageType.BALANCE_REPLY, success, instanceOfDecision, coinType, nonce);
        this.owner = owner;
        this.spender = spender;
        this.allowance = allowance;
    }

    public String getOwner() {
        return owner;
    }
    public String getSpender() {
        return spender;
    }
    public BigInteger getAllowance() {
        return allowance;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        if (!(obj instanceof AllowanceReply)) return false;
        AllowanceReply other = (AllowanceReply) obj;
        return (
                other.getOwner().equals(owner) &&
                other.getSpender().equals(spender) &&
                other.getAllowance().equals(allowance)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), owner, spender, allowance);
    }
}
