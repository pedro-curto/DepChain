package depchain.common.messaging.library;

import depchain.common.messaging.ClientReplyMessage;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.MessageType;

import java.math.BigInteger;
import java.util.Objects;

public class IsBlackListedReply extends ClientReplyMessage {
    private String owner;
    private String account;
    private boolean isBlackListed;

    public IsBlackListedReply(
            boolean success,
            int instanceOfDecision,
            String owner,
            String account,
            boolean isBlackListed,
            CoinType coinType,
            long nonce,
            int memberPort
    ) {
        super(MessageType.IS_BLACK_LISTED_REPLY,
                success,
                instanceOfDecision,
                coinType,
                nonce,
                memberPort
        );
        this.owner = owner;
        this.account = account;
        this.isBlackListed = isBlackListed;
    }

    public String getOwner() {
        return owner;
    }

    public String getAccount() {
        return account;
    }

    public boolean isBlackListed() {
        return isBlackListed;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        if (!(obj instanceof IsBlackListedReply)) return false;
        IsBlackListedReply other = (IsBlackListedReply) obj;
        return (
                other.getOwner().equals(owner) &&
                        other.getAccount().equals(account) &&
                        other.isBlackListed() == isBlackListed
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), owner, account, isBlackListed);
    }
}
