package depchain.common.messaging;

public class KeyExchangeMessage extends Message {
    private final String publicKey;
    private final String encryptedSessionKey;
    private final String mac;

    public KeyExchangeMessage(long sequenceNumber, String publicKey, String encryptedSessionKey, String mac) {
        super(sequenceNumber, MessageType.KEY_EXCHANGE);
        this.publicKey = publicKey;
        this.encryptedSessionKey = encryptedSessionKey;
        this.mac = mac;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public String getMac() {
        return mac;
    }

    @Override
    public String toString() {
        return "KeyExchangeMessage{" +
                "publicKey='" + publicKey + '\'' +
                ", encryptedSessionKey='" + encryptedSessionKey + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }
}
