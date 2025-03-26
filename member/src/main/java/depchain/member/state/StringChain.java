package depchain.member.state;

import java.util.ArrayList;

public class StringChain {

    // TODO -> this will not be here
    private ArrayList<String> blockchain;

    public StringChain(ArrayList<String> blockchain) {
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
