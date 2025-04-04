package depchain.common.domain;

public class ConsensusBlock implements ConsensusObject {

    private Block block;

    public ConsensusBlock(Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsensusBlock)) return false;
        ConsensusBlock that = (ConsensusBlock) o;
        return block.equals(that.block);
    }

    @Override
    public int hashCode() {
        return block.hashCode();
    }

    @Override
    public String toString() {
        return block.toString();
    }
}
