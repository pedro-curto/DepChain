package depchain.member;

import depchain.client.domain.Client;
import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.member.domain.Member;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

import static depchain.member.TestUtils.*;

public class ByzantineBehaviourTest {
	private static final int BASE_MEMBER_PORT = 5001;
	private static final int CLIENT_PORT = 2000;
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

}
//    @Test
//    void testConsensusWithWrongWriteAcceptByzantine() throws Exception {
//        ExecutorService ex = Executors.newCachedThreadPool();
//        this.executor = ex;
//        // start client
//        Client client = startClient("paulo", CLIENT_PORT, memberInfo, executor);
//        this.client = client;
//
//        // starts members and byzantine process
//        Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo, executor);
//        Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo, executor);
//        Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo, executor);
//        Member byzantine = startByzantineMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, "wrong-write-accept", executor);
//        this.members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, byzantine));
//        // wait a bit for the system to boot, sessions established, etc
//        Thread.sleep(2000);
//        // client sends append request
//        client.sendAppend("test-value");
//
//        // check consensus is reached among honest nodes
//        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
//            Assertions.assertTrue(leader.getStringChain().contains("test-value"),
//                    "Leader should have the value");
//            Assertions.assertTrue(honest1.getStringChain().contains("test-value"),
//                    "Honest1 should have the value");
//            Assertions.assertTrue(honest2.getStringChain().contains("test-value"),
//                    "Honest2 should have the value");
//        });
//
//    }
//
//    @Test
//    void testConsensusWithByzantinePerfectLink() throws Exception {
//        ExecutorService ex = Executors.newCachedThreadPool();
//        this.executor = ex;
//        // start client
//        Client client = startClient("paulo", CLIENT_PORT, memberInfo, executor);
//        this.client = client;
//
//        // starts members (all with byzantine perfect link that can delay messages)
//        Member leader = startByzantineMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo, "byz-perfect-link", executor);
//        Member honest1 = startByzantineMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo, "byz-perfect-link", executor);
//        Member honest2 = startByzantineMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo, "byz-perfect-link", executor);
//        Member byzantine = startByzantineMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, "byz-perfect-link", executor);
//        this.members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, byzantine));
//        List<Member> shouldHaveValue = new ArrayList<>(Arrays.asList(leader, honest1, honest2));
//        // wait a bit for the system to boot, sessions established, etc
//        Thread.sleep(2000);
//        // send many append requests and check if consensus is reached
//        sendAppendAndCheck(client, "test-value", shouldHaveValue);
//        sendAppendAndCheck(client, "test-value2", shouldHaveValue);
//        sendAppendAndCheck(client, "test-value3", shouldHaveValue);
//        sendAppendAndCheck(client, "test-value4", shouldHaveValue);
//        sendAppendAndCheck(client, "test-value5", shouldHaveValue);
//    }
//
//
//}