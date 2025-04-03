package depchain.common.messaging.library;

import depchain.common.messaging.CoinType;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

public class BalanceOfMessage extends Message {
	private String address;

	public BalanceOfMessage(String address, int port) {
		super(MessageType.BALANCE_OF, port);
		this.address = address;
	}

	public BalanceOfMessage(String address, int port, CoinType coinType) {
		super(MessageType.BALANCE_OF, port, coinType);
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	@Override
	public String toString() {
		return "BalanceOfMessage{" +
				"address='" + address + '\'' +
				", port=" + getPort() +
				", type=" + getType() +
				'}';
	}
}