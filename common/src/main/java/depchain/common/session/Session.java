package depchain.common.session;

import javax.crypto.SecretKey;
import java.net.DatagramSocket;

public class Session {
    private final String sessionId;
    private final SecretKey secretKey;
    private final DatagramSocket socket;

    public Session(String sessionId, SecretKey secretKey, DatagramSocket socket) {
        this.sessionId = sessionId;
        this.secretKey = secretKey;
        this.socket = socket;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

}
