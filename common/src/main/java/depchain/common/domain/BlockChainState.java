package depchain.common.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockChainState {
    private Map<String, Account> accounts = new HashMap<>();

    public BlockChainState(List<Account> accounts) {
        accounts.forEach(account -> this.accounts.put(account.getAddress(), account));
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public Account getAccount(String address) {
        return new Account(accounts.get(address));
    }
}
