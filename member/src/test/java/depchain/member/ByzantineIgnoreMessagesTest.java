package depchain.member;

import depchain.client.domain.Client;
import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.common.messaging.AppendMessage;
import depchain.member.byzantine.NoAnswerByzantine;
import depchain.member.domain.Member;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

public class ByzantineIgnoreMessagesTest {

    private static final int BASE_MEMBER_PORT = 5001;
    private static final int CLIENT_PORT = 2000;
    private List<Entity> members;
    private List<Entity> clients;
    private ExecutorService executor;

    @BeforeEach
    void setup() throws Exception {
        String userDir = System.getProperty("user.dir");
        System.out.println("Current path: " + System.getProperty("user.dir"));
        members = CommonUtils.loadMembership(userDir + "/membership/membership.txt");
        clients = CommonUtils.loadMembership(userDir + "/membership/client.txt");
        // executor that runs member and client threads
        executor = Executors.newCachedThreadPool();
    }

    //@AfterEach
    //void cleanup() {
    //    executor.shutdownNow();
    //}

    @Test
    void testConsensusWithIgnoringByzantine() throws Exception {
        // start client
        Client client = startClient("paulo", CLIENT_PORT, members);

        // starts members and byzantine process
        Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, members, clients);
        Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, members, clients);
        Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, members, clients);
        Member byzantine = startByzantineMember("dybizantino", BASE_MEMBER_PORT + 3, members, clients);
        // wait a bit for the system to boot, sessions established, etc
        Thread.sleep(2000);
        // client sends append request
        client.sendAppend("test-value");

        // Verify consensus is reached among honest nodes
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertTrue(leader.getBlockchainState().contains("test-value"),
                    "Leader should have the value");
            Assertions.assertTrue(honest1.getBlockchainState().contains("test-value"),
                    "Honest1 should have the value");
            Assertions.assertTrue(honest2.getBlockchainState().contains("test-value"),
                    "Honest2 should have the value");
        });

        // verify Byzantine node does not have the value
        Awaitility.await().during(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            return !byzantine.getBlockchainState().contains("test-value");
        });
    }

    // Helper methods
    private Member startMember(String name, int port,
                               List<Entity> members, List<Entity> clients) throws Exception {
        Member member = new Member(name, members, clients, port, "localhost", true);
        executor.submit(() -> {
            try {
                member.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return member;
    }

    private Member startByzantineMember(String name, int port,
                                        List<Entity> members, List<Entity> clients) throws Exception {
        Member member = new NoAnswerByzantine(name, members, clients, port, "localhost", true);
        executor.submit(() -> {
            try {
                member.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return member;
    }

    private Client startClient(String name, int port, List<Entity> members) throws Exception {
        Client client = new Client(name, port, members, true);
        executor.submit(() -> {
            try {
                client.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return client;
    }
}