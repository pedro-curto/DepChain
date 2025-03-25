package depchain.member.state;

import depchain.common.domain.Account;

import java.util.ArrayList;
import java.util.List;

public class BlockchainState {

    // TODO -> this will not be here
    private ArrayList<String> blockchain;

    private List<Account> accounts;

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
