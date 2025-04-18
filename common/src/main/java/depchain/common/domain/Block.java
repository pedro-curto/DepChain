package depchain.common.domain;

import depchain.common.Security;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Block {
    String hash;
    String previousHash;
    long blockNumber;
    long timestamp;
    List<Transaction> transactions = new ArrayList<>();
    // only for genesis block (for accounts)
    BlockChainState state = null; // has to store the state it had when appended

    public Block(String hash, String previousHash, List<Transaction> transactions, BlockChainState state) {
        // for GENESIS BLOCK
        this.hash = hash;
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.blockNumber = 0;
        this.state = state;
        this.timestamp = System.currentTimeMillis();
    }

    public Block(String hash, String previousHash, List<Transaction> transactions) {
        // for GENESIS BLOCK
        this.hash = hash;
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.blockNumber = 0;
        this.state = null;
        this.timestamp = System.currentTimeMillis();
    }

    public Block(String previousHash, List<Transaction> transactions, long blockNumber, long timestamp) {
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.blockNumber = blockNumber;
        this.timestamp = timestamp;
        this.calculateHash();
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
    public long getBlockNumber() {
        return blockNumber;
    }
    public long getTimestamp() {
        return timestamp;
    }

    public void calculateHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(previousHash);
        sb.append(blockNumber);
        sb.append(timestamp);
        // txs
        sb.append(transactions.stream()
                .map(Transaction::toString)
                .reduce("", String::concat));
        // set hash
        this.hash = Security.makeDigest(sb.toString());
    }

    @Override
    public boolean equals(Object o) {
        // DOES NOT COMPARE BLOCKCHAINSTATE ATTRIBUTE
        if (this == o) return true;
        if (!(o instanceof Block)) return false;
        Block that = (Block) o;
        return blockNumber == that.blockNumber &&
                timestamp == that.timestamp &&
                hash.equals(that.hash) &&
                previousHash.equals(that.previousHash) &&
                transactions.equals(that.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, previousHash, blockNumber, timestamp, transactions);
    }

    @Override
    public String toString() {
        return "BLOCK {hash: " + hash + ", previousHash: " + previousHash + ", transactions: " + transactions + ", state: " + state +
                ", blockNumber: " + blockNumber + ", timestamp: " + timestamp + "}";
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}