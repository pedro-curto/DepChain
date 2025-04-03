package depchain.common.domain;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class BlockChainState {
    private Map<String, Account> accounts = new HashMap<>();
    // each owner (an address) has a map of what value he allowed other addresses (spenders) to spend
    private final HashMap<String, HashMap<String, BigInteger>> allowances;
    private Block lastBlock;

    public BlockChainState(List<Account> accounts) {
        accounts.forEach(account -> this.accounts.put(account.getAddress(), account));
        this.allowances = new HashMap<>();
    }

    public BlockChainState(List<Account> accounts, Block lastBlock) {
        this.accounts = new HashMap<>();
        this.allowances = new HashMap<>();
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

    public BigInteger getAllowance(String owner, String spender) {
        if (!allowances.containsKey(owner)) {
            return BigInteger.ZERO;
        }
        if (!allowances.get(owner).containsKey(spender)) {
            return BigInteger.ZERO;
        }
        return allowances.get(owner).get(spender);
    }

    public void addAllowanceToOwner(String owner, String spender, BigInteger value) {
        if (!allowances.containsKey(owner)) {
            allowances.put(owner, new HashMap<>());
        }
        allowances.get(owner).put(spender, value);
    }

    private boolean canSpendAllowance(String owner, String spender, BigInteger value) {
        Account spenderAccount = getAccount(spender);
        if (spenderAccount.getBalance().compareTo(value) < 0) {
            return false;
        }
        if (!allowances.containsKey(owner)) {
            return false;
        }
        if (!allowances.get(owner).containsKey(spender)) {
            return false;
        }
        BigInteger currentValue = allowances.get(owner).get(spender);
        return currentValue.compareTo(value) >= 0;
    }

    public boolean spendAllowance(String owner, String spender, BigInteger amount) {
        if (canSpendAllowance(owner,spender,amount)) {
            return false;
        }
        return true;
    }
}
