package depchain.common.domain;

import java.util.List;

public class BlockChainState {
    private List<Account> accounts;

    public BlockChainState(List<Account> accounts) {
        this.accounts = accounts;
    }

    public List<Account> getAccounts() {
        return accounts;
    }
}
