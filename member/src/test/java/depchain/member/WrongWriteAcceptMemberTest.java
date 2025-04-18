package depchain.member;

import depchain.client.domain.Client;
import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.common.messaging.CoinType;
import depchain.member.domain.Member;
import org.junit.jupiter.api.AfterEach;
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

public class WrongWriteAcceptMemberTest {
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
	void testConsensusWithWrongWriteAcceptByzantine() throws Exception {
		this.executor = Executors.newCachedThreadPool();
		// start clients
		Client paulo = startClient("paulo", BASE_CLIENT_PORT, memberInfo, executor);
		Client joao = startClient("joao", BASE_CLIENT_PORT + 1, memberInfo, executor);
		Client pedro = startClient("pedro", BASE_CLIENT_PORT + 2, memberInfo, executor);
		this.clients = new ArrayList<>(Arrays.asList(paulo, joao, pedro));
		// starts members and byzantine process
		Member leader = startMember("pedroribeiro", BASE_MEMBER_PORT, memberInfo, clientInfo, executor);
		Member honest1 = startMember("pedrocurto", BASE_MEMBER_PORT + 1, memberInfo, clientInfo, executor);
		Member honest2 = startMember("rodrigogreedy", BASE_MEMBER_PORT + 2, memberInfo, clientInfo, executor);
		Member byzantine = startByzantineMember("dybizantino", BASE_MEMBER_PORT + 3, memberInfo, clientInfo, "wrong-write-accept", executor);
		this.members = new ArrayList<>(Arrays.asList(leader, honest1, honest2, byzantine));
		// wait a bit for the system to boot, sessions established, etc
		Thread.sleep(5000);
		// attempt normal behaviour:
		TestUtils.testTransfer(paulo, joao, BigInteger.valueOf(1000), new BigInteger("9999999000"),
				BigInteger.valueOf(1000), CoinType.ISTCOIN);
		// balances -> paulo: 9999999000, joao: 1000, pedro: 0

		BigInteger allowance = BigInteger.valueOf(1000);
		BigInteger baseOwnerBalance = new BigInteger("9999999000");
		BigInteger baseToBalance = BigInteger.valueOf(1000);
		BigInteger expectedOwnerBalance = baseOwnerBalance.subtract(allowance);
		BigInteger expectedToBalance = baseToBalance.add(allowance);
		// owner, spender, to, amount, expectedFromBalance, expectedToBalance, coinType
		TestUtils.testAllowanceMechanism(paulo, pedro, joao, allowance, expectedOwnerBalance, expectedToBalance, CoinType.ISTCOIN);
	}

}