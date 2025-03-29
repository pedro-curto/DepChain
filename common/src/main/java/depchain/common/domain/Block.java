package depchain.common.domain;

import java.util.ArrayList;
import java.util.List;

public class Block {
    String hash;
    String previousHash;
    List<Transaction> transactions = new ArrayList<>();
    BlockChainState state; // has to store the state it had when appended

    public Block(String hash, String previousHash, List<Transaction> transactions,BlockChainState state) {
        this.hash = hash;
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.state = state;
    }

    public String getHash() {
        return hash;
    }
    public String getPreviousHash() {
        return previousHash;
    }
    public List<Transaction> getTransactions() {
        return transactions;
    }
    public BlockChainState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "BLOCK {hash: " + hash + ", previousHash: " + previousHash + ", transactions: " + transactions + ", state: " + state + "}";
    }
}