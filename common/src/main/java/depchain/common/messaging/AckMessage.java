package depchain.common.messaging;

public class AckMessage extends Message{

	public AckMessage(long sequenceNumber) {
		super(sequenceNumber, MessageType.ACK);
	}
}
