package depchain.common.messaging.library;

import depchain.common.messaging.CoinType;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

public class IsBlackListedMessage extends Message {

    private String account;

    public IsBlackListedMessage(String account, int port, CoinType coinType) {
        super(MessageType.IS_BLACK_LISTED, port, coinType);
        this.account = account;
    }

    public String getAccount() {
        return this.account;
    }
}
