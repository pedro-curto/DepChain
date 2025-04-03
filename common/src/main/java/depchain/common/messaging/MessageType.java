package depchain.common.messaging;

public enum MessageType {
	APPEND,
	ACK,
	CLIENT_REPLY,
	KEY_EXCHANGE,
	READ,
	WRITE,
	STATE,
	COLLECTED,
	ACCEPT,
	CLIENT,
	// blockchain operations
	BALANCE_OF, TRANSFER, APPROVE, ALLOWANCE, TRANSFER_FROM, IS_BLACK_LISTED
}
