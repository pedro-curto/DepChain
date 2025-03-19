package depchain.member;

import depchain.client.domain.Client;
import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.member.domain.Member;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static depchain.member.TestUtils.*;

public class RegularTest {

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
	void testNormalBehaviour() throws Exception {
		// start client
		ExecutorService ex = Executors.newCachedThreadPool();
		this.executor = ex;
		//TestUtils testUtils = new TestUtils(ex);
		Client client = startClient("paulo", CLIENT_PORT, memberInfo, executor);
		this.client = client;

		// starts members and byzantine process
		Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo, executor);
		Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo, executor);
		Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo, executor);
		Member honest3 = startMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, executor);
		members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, honest3));
		// wait a bit for the system to boot, sessions established, etc
		Thread.sleep(5000);
		// client sends some appends and checks if the blockchain of all members contains the value
		sendAppendAndCheck(client, "a", members);
		sendAppendAndCheck(client, "b", members);
		sendAppendAndCheck(client, "c", members);
		sendAppendAndCheck(client, "d", members);
		sendAppendAndCheck(client, "e", members);
	}

}
