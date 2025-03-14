package depchain.member;

import depchain.client.domain.Client;
import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.member.byzantine.*;
import depchain.member.domain.Member;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

public class ByzantineBehaviourTest {

    private static final int BASE_MEMBER_PORT = 5001;
    private static final int CLIENT_PORT = 2000;
    private List<Entity> memberInfo;
    private List<Entity> clientInfo;
    private ExecutorService executor;
    private Client client;
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
        client.stop();
        // kills member threads
        for (Member member : members) {
            member.stop();
        }
        executor.shutdownNow();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        if (!finished) {
            System.err.println("Executor did not shut down in time!");
        }
    }
    @Test
    void testConsensusWithIgnoringByzantine() throws Exception {
        // start client
        ExecutorService ex = Executors.newCachedThreadPool();
        this.executor = ex;
        //TestUtils testUtils = new TestUtils(ex);
        Client client = startClient("paulo", CLIENT_PORT, memberInfo);
        this.client = client;

        // starts members and byzantine process
        Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo);
        Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo);
        Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo);
        Member byzantine = startByzantineMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, "no-answer");
        members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, byzantine));
        // wait a bit for the system to boot, sessions established, etc
        Thread.sleep(5000);
        // client sends append request
        client.sendAppend("test-value");

        // check consensus is reached among honest nodes
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertTrue(leader.getBlockchainState().contains("test-value"),
                    "Leader should have the value");
            Assertions.assertTrue(honest1.getBlockchainState().contains("test-value"),
                    "Honest1 should have the value");
            Assertions.assertTrue(honest2.getBlockchainState().contains("test-value"),
                    "Honest2 should have the value");
        });

    }

    @Test
    void testConsensusWithSpamByzantine() throws Exception {
        ExecutorService ex = Executors.newCachedThreadPool();
        this.executor = ex;
        // start client
        Client client = startClient("paulo", CLIENT_PORT, memberInfo);
        this.client = client;

        // starts members and byzantine process
        Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo);
        Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo);
        Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo);
        Member byzantine = startByzantineMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, "spam");
        this.members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, byzantine));
        // wait a bit for the system to boot, sessions established, etc
        Thread.sleep(2000);
        // client sends append request
        client.sendAppend("test-value");

        // check consensus is reached among honest nodes
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertTrue(leader.getBlockchainState().contains("test-value"),
                    "Leader should have the value");
            Assertions.assertTrue(honest1.getBlockchainState().contains("test-value"),
                    "Honest1 should have the value");
            Assertions.assertTrue(honest2.getBlockchainState().contains("test-value"),
                    "Honest2 should have the value");
        });

    }

    @Test
    void testConsensusWithFakeSignature() throws Exception {
        ExecutorService ex = Executors.newCachedThreadPool();
        this.executor = ex;
        //TestUtils testUtils = new TestUtils(ex);
        // start client
        Client client = startClient("paulo", CLIENT_PORT, memberInfo);
        this.client = client;

        // starts members and byzantine process
        Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo);
        Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo);
        Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo);
        Member byzantine = startByzantineMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, "fake-signature");
        this.members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, byzantine));
        // wait a bit for the system to boot, sessions established, etc
        Thread.sleep(2000);
        // client sends append request
        client.sendAppend("test-value");

        // check consensus is reached among honest nodes
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertTrue(leader.getBlockchainState().contains("test-value"),
                    "Leader should have the value");
            Assertions.assertTrue(honest1.getBlockchainState().contains("test-value"),
                    "Honest1 should have the value");
            Assertions.assertTrue(honest2.getBlockchainState().contains("test-value"),
                    "Honest2 should have the value");
        });

        // assert if the leader caught an invalid signature
        Assertions.assertTrue(leader.caughtInvalidSignature(),
                "Leader should have caught invalid signature");
    }

    @Test
    void testConsensusWithWrongState() throws Exception {
        ExecutorService ex = Executors.newCachedThreadPool();
        this.executor = ex;
        //TestUtils testUtils = new TestUtils(ex);
        // start client
        Client client = startClient("paulo", CLIENT_PORT, memberInfo);
        this.client = client;

        // starts members and byzantine process
        Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo);
        Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo);
        Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo);
        Member byzantine = startByzantineMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, "wrong-state");
        this.members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, byzantine));
        // wait a bit for the system to boot, sessions established, etc
        Thread.sleep(2000);
        // client sends append request
        client.sendAppend("test-value");

        // check consensus is reached among honest nodes
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertTrue(leader.getBlockchainState().contains("test-value"),
                    "Leader should have the value");
            Assertions.assertTrue(honest1.getBlockchainState().contains("test-value"),
                    "Honest1 should have the value");
            Assertions.assertTrue(honest2.getBlockchainState().contains("test-value"),
                    "Honest2 should have the value");
        });

        // client sends append request with a new test value
        client.sendAppend("test-value2");

        // byzantine process CoordinatedWrongStateByzantine will send a fake state message based on the previous
        // value ("test-value" in this case) and a bigger timestamp, and start echoing writes and accepts.
        // we want to check if consensus still works in this scenario
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertTrue(leader.getBlockchainState().contains("test-value2"),
                    "Leader should have the value");
            Assertions.assertTrue(honest1.getBlockchainState().contains("test-value2"),
                    "Honest1 should have the value");
            Assertions.assertTrue(honest2.getBlockchainState().contains("test-value2"),
                    "Honest2 should have the value");
        });

    }


    // auxiliar methods
    public Member startMember(String name, int port,
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

    public Member startByzantineMember(String name, int port,
                                       List<Entity> members, List<Entity> clients, String byzantineType) throws Exception {
        Member byzantineMember;
        switch (byzantineType) {
            case "no-answer":
                byzantineMember = new NoAnswerByzantine(name, members, clients, port, "localhost", false);
                break;
            case "fake-signature":
                byzantineMember = new FakeSignatureByzantine(name, members, clients, port, "localhost", false);
                break;
            case "spam":
                byzantineMember = new SpamByzantine(name, members, clients, port, "localhost", false);
                break;
            case "wrong-state":
                byzantineMember = new CoordinatedWrongStateByzantine(name, members, clients, port, "localhost", false);
                break;
            case "replay-signature":
                byzantineMember = new ReplaySignatureByzantine(name, members, clients, port, "localhost", false);
                break;
            case "wrong-write-accept":
                byzantineMember = new WrongWriteAcceptByzantine(name, members, clients, port, "localhost", false);
                break;
            default:
                throw new IllegalArgumentException("Invalid Byzantine type");
        }
        executor.submit(() -> {
            try {
                byzantineMember.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return byzantineMember;
    }

    public Client startClient(String name, int port, List<Entity> members) throws Exception {
        Client client = new Client(name, port, members, true, true);
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