package depchain.common.session;

import javax.crypto.SecretKey;
import java.net.DatagramSocket;

public class Session {
    private final SecretKey secretKey;
    private final int port;
    private final String address;
    private long receiveCounter;
    private long sendCounter;

    public Session(SecretKey secretKey, int port, String address) {
        this.secretKey = secretKey;
        this.port = port;
        this.address = address;
        this.receiveCounter = 0;
        this.sendCounter = 0;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public int getPort() { return port; }

    public String getAddress() { return address; }

    public long getReceiveCounter() { return receiveCounter; }

    public long getSendCounter() { return sendCounter; }

    public void setReceiveCounter(long receiveCounter) { this.receiveCounter = receiveCounter; }

    public void setSendCounter(long sendCounter) { this.sendCounter = sendCounter; }

}
