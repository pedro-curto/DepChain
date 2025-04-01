package depchain.common.domain;

import java.util.*;
import java.util.stream.Collectors;

public class BlockChainState {
    private Map<String, Account> accounts = new HashMap<>();
    private Block lastBlock;

    public BlockChainState(List<Account> accounts) {
        accounts.forEach(account -> this.accounts.put(account.getAddress(), account));
    }

    public BlockChainState(List<Account> accounts, Block lastBlock) {
        this.accounts = new HashMap<>();
        accounts.forEach(account -> this.accounts.put(account.getAddress(), account));
        this.lastBlock = lastBlock;
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public Account getAccount(String address) {
        if (!accounts.containsKey(address)) {
            System.out.println("Existing accounts: " + accounts.keySet());
            throw new IllegalArgumentException("Account with address " + address + " does not exist.");
        }
        return accounts.get(address);
        // new account? assim o balance vai ser alterado
        //return new Account(accounts.get(address));
    }

    public BlockChainState copy() {
        List<Account> copiedAccounts = accounts.values().stream()
                .map(Account::new)
                .collect(Collectors.toList());
        return new BlockChainState(copiedAccounts);
    }

    @Override
    public String toString() {
        return "BLOCKCHAIN STATE {accounts: " + accounts.values().stream()
                .map(Account::toString)
                .collect(Collectors.joining(", ")) + "}";
    }

    public Block getLastBlock() {
        return lastBlock;
    }

	public void setLastBlock(Block block) {
        this.lastBlock = block;
    }

    public void addAccount(Account account) {
        accounts.put(account.getAddress(), account);
    }

    public void removeAccount(String address) {
        accounts.remove(address);
	}
}
