package depchain.common.domain;

import java.util.List;

public class GenesisBlock extends Block {
    private List<Account> accounts;
    private ContractData contractData;

    public GenesisBlock(String hash, String previousHash, List<Transaction> transactions,
                        List<Account> accounts, ContractData contractData) {
        super(hash, previousHash, transactions);
        this.accounts = accounts;
        this.contractData = contractData;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public ContractData getContractData() {
        return contractData;
    }

    @Override
    public String toString() {
        return "GenesisBlock{" +
                "hash='" + getHash() + '\'' +
                ", previousHash='" + getPreviousHash() + '\'' +
                ", transactions=" + getTransactions() +
                ", accounts=" + accounts +
                ", contractData=" + contractData +
                '}';
    }
}
