package depchain.common.domain;

public class Transaction {
    private Account sender;
    private Account recipient;
    private Double amount;
    private String signature;
    private long nonce;

    public Transaction(Account sender, Account recipient, Double amount, String signature, long nonce) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.signature = signature;
        this.nonce = nonce;
    }

    public Account getSender() {
        return sender;
    }

    public Account getRecipient() {
        return recipient;
    }

    public Double getAmount() {
        return amount;
    }

    public String getSignature() {
        return signature;
    }

    public long getNonce() {
        return nonce;
    }

}