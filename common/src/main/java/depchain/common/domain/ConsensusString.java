package depchain.common.domain;

public class ConsensusString implements ConsensusObject {

    private String value;

    public ConsensusString(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsensusString)) return false;
        ConsensusString that = (ConsensusString) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
