package depchain.common.domain;

public class ContractAccount extends Account {

    String storage;
    String evmByteCode;
    EOAccount owner;

    public ContractAccount(String address, long balance, String storage, String evmByteCode) {
        super(address, balance);
        this.storage = storage;
        this.evmByteCode = evmByteCode;
    }
}