package depchain.common.messaging;

public class KeyExchangeMessage extends Message {
    private final String publicKey;
    private final String encryptedSessionKey;
    private final String signature;

    public KeyExchangeMessage(long sequenceNumber, String publicKey, String encryptedSessionKey, String signature) {
        super(sequenceNumber, MessageType.KEY_EXCHANGE);
        this.publicKey = publicKey;
        this.encryptedSessionKey = encryptedSessionKey;
        this.signature = signature;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public String getSignature() { return signature; }

    @Override
    public String toString() {
        return "KeyExchangeMessage{" +
                "publicKey='" + publicKey + '\'' +
                ", encryptedSessionKey='" + encryptedSessionKey + '\'' +
                '}';
    }
}
