package depchain.common.domain;

import java.util.*;
import java.util.stream.Collectors;

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

    public BlockChainState copy() {
        List<Account> copiedAccounts = accounts.values().stream()
                .map(Account::new)
                .collect(Collectors.toList());
        return new BlockChainState(copiedAccounts);
    }
}
