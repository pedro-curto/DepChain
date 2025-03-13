package depchain.member.state;

import java.util.ArrayList;

public class BlockchainState {

    private ArrayList<String> blockchain;

    public BlockchainState(ArrayList<String> blockchain) {
        this.blockchain = blockchain;
    }

    public synchronized void appendString(String str) {
        blockchain.add(str);
        System.err.println("[BLOCKCHAIN]: " + blockchain);
    }

    public boolean contains(String str) {
        return blockchain.contains(str);
    }
}
