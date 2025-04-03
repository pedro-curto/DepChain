package depchain.common.messaging.library;

import depchain.common.messaging.CoinType;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

public class IsBlackListedMessage extends Message {

    private String owner;

    private String account;

    public IsBlackListedMessage(String owner, String account, int port, CoinType coinType) {
        super(MessageType.IS_BLACK_LISTED, port, coinType);
        this.owner = owner;
        this.account = account;
    }

    public String getOwner() {
        return owner;
    }

    public String getAccount() {
        return this.account;
    }
}
