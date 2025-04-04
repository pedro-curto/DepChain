package depchain.common.domain;

import java.util.Objects;

public class ValueTimeStampPair2 {

    private int timestamp;
    private final ConsensusObject value;
    private String clientSignature;
    private int clientPort;
    private long nonce;

    public ValueTimeStampPair2(int timestamp, ConsensusObject value, int clientPort, long nonce) {
        this.value = value;
        this.timestamp = timestamp;
        this.clientPort = clientPort;
        this.nonce = nonce;
    }

    public ValueTimeStampPair2(int epoch, ConsensusObject value) {
        this.timestamp = epoch;
        this.value = value;
    }

    public int getClientPort() {
        return clientPort;
    }

    public long getNonce() {
        return nonce;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public ConsensusObject getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "<" + timestamp + "," + value + "," + clientPort + "," + nonce + ">";
    }

    public void setClientSignature(String signature) {
        this.clientSignature = signature;
    }

    public String getClientSignature() {
        return clientSignature;
    }

    // we need equals and hashCode to group VTPs in the write and accept phases
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        ValueTimeStampPair2 other = (ValueTimeStampPair2) o;
        return this.timestamp == other.timestamp &&
                this.clientPort == other.clientPort &&
                this.nonce == other.nonce &&
                Objects.equals(this.value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value, clientPort, nonce);
    }
}
