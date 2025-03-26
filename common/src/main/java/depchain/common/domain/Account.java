package depchain.common.domain;

public class Account{
    String address;
    Double balance;

    public Account(String address, Double balance) {}

    public String getAddress() {
        return address;
    }
    public Double getBalance() {
        return balance;
    }
}