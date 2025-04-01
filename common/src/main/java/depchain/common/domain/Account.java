package depchain.common.domain;

public class Account{
    // based on: https://ethereum.org/en/developers/docs/accounts/
    private String address;
    private String name;
    private long balance;
    private long nonce;

    public Account(String address, long balance) {}
    public Account(Account account) {
        this.address = account.address;
        this.balance = account.balance;
        this.name = account.name;
        this.nonce = 0;
    }

    public String getAddress() {
        return address;
    }
    public long getBalance() {
        return balance;
    }
    public String getName() {
        return name;
    }
    public long getNonce() {
        return nonce;
    }
    public void incrementNonce() {
        this.nonce++;
    }

    @Override
    public String toString() {
        return "ACCOUNT {address: " + address + ", balance: " + balance + ", name: " + name + "}";
    }
}