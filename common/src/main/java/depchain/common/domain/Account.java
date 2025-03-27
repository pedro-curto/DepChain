package depchain.common.domain;

public class Account{
    String address;
    Double balance;

    public Account(String address, Double balance) {}
    public Account(Account account) {
        this.address = account.address;
        this.balance = account.balance;
    }

    public String getAddress() {
        return address;
    }
    public Double getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return "ACCOUNT {address: " + address + ", balance: " + balance + "}";
    }
}