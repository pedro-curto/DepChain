package depchain.common.domain;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import depchain.common.JsonAdapter;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.TransactionType;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

public class Transaction {
    // based on: https://ethereum.org/en/developers/docs/transactions/
    private String senderAddr;
    private String spenderAddr; // we need this field to handle TRANSFER_FROMs
    private String recipientAddr;
    private BigInteger amount;
    private String signature;
    private long nonce;
    private TransactionType transactionType;
    private boolean success;
    private CoinType coinType;
    private int clientPort;

    public Transaction(String senderAddr,
                       String spenderAddr,
                       String recipientAddr,
                       BigInteger amount,
                       String signature,
                       long nonce,
                       TransactionType transactionType,
                       CoinType coinType,
                       int clientPort
    ) {
        this.senderAddr = senderAddr;
        this.spenderAddr = spenderAddr;
        this.recipientAddr = recipientAddr;
        this.amount = amount;
        this.signature = signature;
        this.nonce = nonce;
        this.transactionType = transactionType;
        this.coinType = coinType;
        this.clientPort = clientPort;
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

    public String getSpender() {
        return spenderAddr;
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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public boolean getSuccess() {
        return success;
    }

    public CoinType getCoinType() {
        return coinType;
    }

    public int getClientPort() {
        return clientPort;
    }


    public void setStatus(boolean success) {
        this.success = success;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        Transaction other = (Transaction) o;
        return this.senderAddr.equals(other.senderAddr) &&
                this.spenderAddr.equals(other.spenderAddr) &&
                this.recipientAddr.equals(other.recipientAddr) &&
                this.amount.equals(other.amount) &&
                this.nonce == other.nonce &&
                this.transactionType == other.transactionType &&
                this.success == other.success &&
                this.coinType == other.coinType;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "senderAddr='" + senderAddr + '\'' +
                "spenderAddr='" + spenderAddr + '\'' +
                ", recipientAddr='" + recipientAddr + '\'' +
                ", amount=" + amount +
                ", nonce=" + nonce +
                ", transactionType=" + transactionType +
                ", success=" + success +
                ", coinType=" + coinType +
                '}';
    }
    /*
    from TransferMessage
    public String getDataToSign() {
		if (transactionType == TransactionType.TRANSFER_FROM) {
			return from + spender + to + value + nonce + transactionType + clientPort;
		}
		return from + to + value + nonce + transactionType + clientPort;
	}
     */
	public String getDataToSign() {
        if (transactionType == TransactionType.TRANSFER_FROM) {
            return senderAddr + spenderAddr + recipientAddr + amount + nonce + transactionType + clientPort;
        }
        return senderAddr + recipientAddr + amount + nonce + transactionType + clientPort;
    }
}