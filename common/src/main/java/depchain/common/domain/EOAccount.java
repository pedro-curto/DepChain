package depchain.common.domain;

import java.security.KeyPair;

public class EOAccount extends Account {
    private final KeyPair keyPair;

    public EOAccount(String address, long balance, KeyPair keyPair) {
        super(address, balance);
        this.keyPair = keyPair;
    }
}