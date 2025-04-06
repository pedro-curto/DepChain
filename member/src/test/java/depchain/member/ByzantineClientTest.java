package depchain.member;

import depchain.client.domain.ByzantineClient;
import depchain.client.domain.Client;
import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.common.domain.Transaction;
import depchain.common.messaging.CoinType;
import depchain.common.messaging.library.TransferMessage;
import depchain.member.domain.Member;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static depchain.member.TestUtils.*;

public class ByzantineClientTest {
    // remaining things
    private static final int BASE_MEMBER_PORT = 5001;
    private static final int BASE_CLIENT_PORT = 2000;
    private List<Entity> memberInfo;
    private List<Entity> clientInfo;
    private ExecutorService executor;
    private List<Client> clients;
    private List<Member> members;

    @BeforeEach
    void setup() throws Exception {
        String userDir = System.getProperty("user.dir");
        System.out.println("Current path: " + System.getProperty("user.dir"));
        memberInfo = CommonUtils.loadMembership(userDir + "/membership/membership.txt");
        clientInfo = CommonUtils.loadMembership(userDir + "/membership/client.txt");
        // executor that runs member and client threads
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        for (Client client : clients) {
            client.stop();
        }
        // kills member threads
        for (Member member : members) {
            member.stop();
        }
        executor.shutdownNow();
        boolean finished = executor.awaitTermination(2, TimeUnit.SECONDS);
        if (!finished) {
            System.err.println("Executor did not shut down in time!");
        }
    }

    @Test
    void testWithWrongSignatureClient() throws Exception {
        // start client
        Client byzantineClient = startByzantineClient("paulo", BASE_CLIENT_PORT, memberInfo, 1, executor);
        this.clients = new ArrayList<>(Arrays.asList(byzantineClient));

        // starts members and byzantine process
        Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo, executor);
        Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo, executor);
        Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo, executor);
        Member honest3 = startMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, executor);
        this.members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, honest3));
        // wait a bit for the system to boot, sessions established, etc
        Thread.sleep(5000);

        // starts by asserting that the client has enough funds for the transfer
        byzantineClient.sendGetBalance("paulo", CoinType.ISTCOIN);
        BigInteger transferValue = BigInteger.valueOf(1000);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                    // waits until the value is different from null
                    Assertions.assertNotNull(byzantineClient.getLastBalance(),
                            "Client should have received a balance");
        });
        BigInteger balance = byzantineClient.getLastBalance();
        System.out.println("(Test) Balance: " + balance);
        Assertions.assertTrue(balance.compareTo(transferValue) > 0);
        byzantineClient.resetReplies();

        // client sends transfer request and we assert that the response back to client is negative
        byzantineClient.sendTransfer("joao", transferValue, CoinType.ISTCOIN);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            // wait until we get a transfer reply and check if its result is false
            Assertions.assertNotNull(byzantineClient.getLastTransferReply(),
                    "Client should have received a reply");

            Assertions.assertFalse(byzantineClient.getLastTransferReply().getSuccess(),
                    "Client should have received a negative reply");
        });

    }

    @Test
    void testWithReplayAttackClient() throws Exception {
        // start client and members
        Client regularClient = startClient("paulo", BASE_CLIENT_PORT, memberInfo, executor);
        ByzantineClient byzantineClient = startByzantineClient("joao", BASE_CLIENT_PORT+1, memberInfo, 2, executor);
        this.clients = new ArrayList<>(Arrays.asList(regularClient, byzantineClient));
        this.members = TestUtils.startHonestMembers(BASE_MEMBER_PORT, memberInfo, clientInfo, executor);
        BigInteger byzCredit = BigInteger.valueOf(2000);
        BigInteger txAmount = BigInteger.valueOf(1000);

        // honest client sends transfer
        regularClient.sendTransfer("pedro", txAmount, CoinType.ISTCOIN);
        // sleeps for 5s
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            // wait until the transactions list in blockchain state's last block is not empty
            Assertions.assertFalse(members.getFirst().getBlockChainState().getLastBlock()
                            .getTransactions().isEmpty(),
                    "Member's BlockchainState's last block should have the executed txs");
        });

        // byzantine member stores the correctly executed transfer to attempt replay
        byzantineClient.setLastExecutedTransaction(members.getFirst().getBlockChainState().getLastBlock()
                .getTransactions().getFirst());
        Transaction tx = byzantineClient.getLastExecutedTransaction();
        System.out.println("(Test) tx: " + tx.toString());

        // attempts replay
        TransferMessage transferMessage = new TransferMessage(
                tx.getSender(),
                tx.getSpender(),
                tx.getRecipient(),
                tx.getAmount(),
                tx.getCoinType(),
                tx.getNonce(),
                tx.getTransactionType(),
                tx.getClientPort()
        );
        transferMessage.setSignature(tx.getSignature());
        System.out.println("(Test) TransferMessage: " + transferMessage);
        System.out.println("(Test) TransferMessage signature: " + transferMessage.getSignature());

        byzantineClient.sendTransfer(transferMessage);
        // asserts that replay attack flag was raised
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertTrue(members.getFirst().getReplayAttack(),
                    "Member should have raised replay attack flag");
        });
    }

}