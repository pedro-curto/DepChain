package depchain.common.messaging.library;

import depchain.common.messaging.CoinType;
import depchain.common.messaging.Message;
import depchain.common.messaging.MessageType;

public class AllowanceMessage extends Message {
	private String owner;
	private String spender;

	public AllowanceMessage(String owner, String spender, int port, CoinType coinType) {
		super(MessageType.ALLOWANCE, port, coinType);
		this.owner = owner;
		this.spender = spender;
	}

	public String getOwner() {
		return owner;
	}

	public String getSpender() {
		return spender;
	}


	@Override
	public String toString() {
		return "AllowanceMessage{" +
				"owner='" + owner + '\'' +
				", spender='" + spender + '\'' +
				", coinType=" + super.getCoinType() +
				'}';
	}
}
