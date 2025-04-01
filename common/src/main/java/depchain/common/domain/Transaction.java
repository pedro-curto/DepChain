package depchain.common.domain;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import depchain.common.JsonAdapter;
import depchain.common.messaging.CoinType;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

public class Transaction {
    // based on: https://ethereum.org/en/developers/docs/transactions/
    private String senderAddr;
    private String recipientAddr;
    private BigInteger amount;
    private String signature;
    private long nonce;
    private TransactionType type;
    private boolean success;
    private CoinType coinType;

    public Transaction(String senderAddr, String recipientAddr, BigInteger amount, String signature,
                       long nonce, TransactionType type, CoinType coinType) {
        this.senderAddr = senderAddr;
        this.recipientAddr = recipientAddr;
        this.amount = amount;
        this.signature = signature;
        this.nonce = nonce;
        this.type = type;
        this.coinType = coinType;
    }

    public enum TransactionType {
        TRANSFER, TRANSFER_FROM, APPROVE
    }

    public void save() {
        JsonObject json = JsonAdapter.serializeTransaction(this);

        // Convert JSON object to string
        Gson gson = new Gson();
        String jsonString = gson.toJson(json);

        // Save to a file
        try (FileWriter file = new FileWriter("transaction.json")) {
            file.write(jsonString);
            file.flush();
            System.out.println("(Transaction) JSON saved successfully!");
        } catch (IOException e) {
            System.err.println("(Transaction) JSON save failed!");
            throw new RuntimeException(e);
        }
    }

    public String getSender() {
        return senderAddr;
    }

    public String getRecipient() {
        return recipientAddr;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public String getSignature() {
        return signature;
    }

    public long getNonce() {
        return nonce;
    }

    public TransactionType getType() {
        return type;
    }

    public boolean getSuccess() {
        return success;
    }

    public CoinType getCoinType() {
        return coinType;
    }


    public void setStatus(boolean success) {
        this.success = success;
    }

}