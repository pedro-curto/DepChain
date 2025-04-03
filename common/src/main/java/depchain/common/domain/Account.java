package depchain.common.domain;

import java.math.BigInteger;

public class Account {
    // based on: https://ethereum.org/en/developers/docs/accounts/
    private String address;
    private String name;
    private BigInteger balance;
    private long nonce;

    public Account(String address, long balance) {
        this.address = address;
        this.balance = BigInteger.valueOf(balance);
        this.name = "";
        this.nonce = 0;
    }

    public Account(String address, String name, long balance) {
        this.address = address;
        this.name = name;
        this.balance = BigInteger.valueOf(balance);
        this.nonce = 0;
    }

    public Account(Account account) {
        this.address = account.address;
        this.balance = account.balance;
        this.name = account.name;
        this.nonce = 0;
    }

    public String getAddress() {
        return address;
    }
    public BigInteger getBalance() {
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

    public void decreaseBalance(BigInteger value) {
        if (value.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot decrease balance by a negative value!");
        }
        this.balance = this.balance.subtract(value);
    }

    public void increaseBalance(BigInteger value) {
        if (value.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot increase balance by a negative value!");
        }
        this.balance = this.balance.add(value);
    }

    public void setBalance(BigInteger value) {
        this.balance = value;
    }
}