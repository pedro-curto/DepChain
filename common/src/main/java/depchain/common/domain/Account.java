package depchain.common.domain;

public class Account{
    private String address;
    private String name;
    private Double balance;

    public Account(String address, Double balance) {}
    public Account(Account account) {
        this.address = account.address;
        this.balance = account.balance;
        this.name = account.name;
    }

    public String getAddress() {
        return address;
    }
    public Double getBalance() {
        return balance;
    }
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ACCOUNT {address: " + address + ", balance: " + balance + ", name: " + name + "}";
    }
}