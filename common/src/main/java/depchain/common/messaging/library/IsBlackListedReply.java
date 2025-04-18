package depchain.common.messaging.library;

import depchain.common.messaging.ClientReplyMessage;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.MessageType;

import java.math.BigInteger;
import java.util.Objects;

public class IsBlackListedReply extends ClientReplyMessage {
    private String account;
    private boolean isBlackListed;

    public IsBlackListedReply(
            boolean success,
            int instanceOfDecision,
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
        this.account = account;
        this.isBlackListed = isBlackListed;
    }


    public String getAccount() {
        return account;
    }

    public boolean isBlackListed() {
        return isBlackListed;
    }

    public String getDataToSign() {
        return account + isBlackListed;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        if (!(obj instanceof IsBlackListedReply)) return false;
        IsBlackListedReply other = (IsBlackListedReply) obj;
        return (other.getAccount().equals(account) &&
                        other.isBlackListed() == isBlackListed
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), account, isBlackListed);
    }
}
