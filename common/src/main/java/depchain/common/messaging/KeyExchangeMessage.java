package depchain.common.messaging;

public class KeyExchangeMessage extends Message {
    private final String encryptedSessionKey;
    private final String signature;

    public KeyExchangeMessage(long sequenceNumber, String encryptedSessionKey, String signature) {
        super(sequenceNumber, MessageType.KEY_EXCHANGE);
        this.encryptedSessionKey = encryptedSessionKey;
        this.signature = signature;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public String getSignature() { return signature; }

    @Override
    public String toString() {
        return "KeyExchangeMessage{" +
                "encryptedSessionKey='" + encryptedSessionKey + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
