package depchain.client.domain;

import depchain.common.domain.Entity;
import depchain.common.messaging.library.TransferMessage;

import java.util.List;
import java.util.Random;
import java.util.Base64;

public class ByzantineClient extends Client {
    private final int byzantineType;
    private final Random random = new Random();

    public ByzantineClient(String clientName, int port, List<Entity> members,
                           int byzantineType, boolean testEnvironment) {
        super(clientName, port, members, testEnvironment);
        this.byzantineType = byzantineType;
    }

    @Override
    public String generateSignature(TransferMessage msg) {
        // use the byzantine type to understand if we send a fake signature or not
        if (byzantineType == 1) {
            return generateFakeSignature();
        }
        return super.generateSignature(msg);
    }

    public void sendTransfer(TransferMessage msg) {
        if (byzantineType == 2) {
            super.sendMessageToLeader(msg);
        } else {
            // not supposed to use this method
            throw new UnsupportedOperationException("Not supposed to be used, just for byzantine test");
        }
    }

    private String generateFakeSignature() {
        byte[] fakeBytes = new byte[256];
        random.nextBytes(fakeBytes);
        return Base64.getEncoder().encodeToString(fakeBytes);
    }

//    @Override
//    public void sendAppend(String content) {
//        if ("fake-signature".equals(byzantineType)) {
//            sendAppendWithFakeSignature(content);
//        } else if ("replay-transfer".equals(byzantineType)) {
//            // Implementation for replay transfer
//            replayTransferAttack();
//        } else {
//            super.sendAppend(content);
//        }
//    }
//
//    private void sendAppendWithFakeSignature(String content) {
//        // Create append message with invalid signature
//        AppendMessage msg = new AppendMessage(content, this.getPort(), getNonce());
//        String fakeSignature = generateFakeSignature();
//        msg.setSignature(fakeSignature);
//        sendMessageToLeader(msg);
//    }
//
//    private String generateFakeSignature() {
//        byte[] fakeBytes = new byte[256];
//        random.nextBytes(fakeBytes);
//        return Base64.getEncoder().encodeToString(fakeBytes);
//    }
//
//    public void replayTransferAttack() {
//        // Attempt to send a transfer message with another client's details
//        // but your own signature (which won't match)
//        TransferMessage msg = new TransferMessage(
//                "0xfakeAddress", // Pretend to be another address
//                null,
//                getAddressMap().get("pedroribeiro"), // Some target address
//                BigInteger.valueOf(1000),
//                CoinType.ISTCOIN,
//                getNonce(),
//                TransactionType.TRANSFER,
//                getPort()
//        );
//
//        // Sign with our private key, but pretending to be another address
//        String dataToSign = msg.getDataToSign();
//        String signature = Security.makeDS(dataToSign, getClientKeys().getPrivate());
//        msg.setSignature(signature);
//
//        sendMessageToLeader(msg);
//    }
//
//    protected long getNonce() {
//        return this.nonce;
//    }
//
//    protected Map<String, String> getAddressMap() {
//        return this.addresses;
//    }
//
//    protected int getPort() {
//        return this.port;
//    }
//
//    protected KeyPair getClientKeys() {
//        return this.clientKeys;
//    }
}