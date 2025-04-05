package depchain.member;

import depchain.client.domain.Client;
import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.common.messaging.CoinType;
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

public class RegularTest {

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
		boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
		if (!finished) {
			System.err.println("Executor did not shut down in time!");
		}
	}

	@Test
	void testBlockChainNormalBehaviour() throws Exception {
		// start client and members
		ExecutorService ex = Executors.newCachedThreadPool();
		this.executor = ex;
		Client paulo = startClient("paulo", CLIENT_PORT, memberInfo, executor);
		Client joao = startClient("joao", CLIENT_PORT + 1, memberInfo, executor);
		Client pedro = startClient("pedro", CLIENT_PORT + 2, memberInfo, executor);
		this.clients = new ArrayList<>(Arrays.asList(paulo, joao, pedro));
		this.members = TestUtils.startHonestMembers(BASE_MEMBER_PORT, memberInfo, clientInfo, executor);

		// -- ISTCOIN TRANSFERS -- //
		// all clients transfer some coins to each other
		TestUtils.testTransfer(paulo, joao, BigInteger.valueOf(1000), new BigInteger("9999999000"),
				BigInteger.valueOf(1000), CoinType.ISTCOIN);
		TestUtils.testTransfer(joao, pedro, BigInteger.valueOf(200),
				BigInteger.valueOf(800), BigInteger.valueOf(200), CoinType.ISTCOIN);
		TestUtils.testTransfer(pedro, paulo, BigInteger.valueOf(100),
				BigInteger.valueOf(100), new BigInteger("9999999100"), CoinType.ISTCOIN);

		// current balances: paulo: 9999999100, joao: 800, pedro: 100
		// paulo approves pedro to spend 1000 ISTCOIN (and check op. success)
		paulo.sendApprove(pedro.getClientName(), BigInteger.valueOf(1000), CoinType.ISTCOIN);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(paulo.getLastTransferReply());
		});
		Assertions.assertTrue(paulo.getLastTransferReply().getSuccess());
		System.out.println("(TEST) paulo approved pedro to spend 1000 ISTCOIN");
		paulo.setLastTransferReply(null);

		// we check pedro's allowance
		pedro.sendGetAllowance(paulo.getClientName(), pedro.getClientName(), CoinType.ISTCOIN);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(pedro.getLastAllowance());
		});
		Assertions.assertEquals(pedro.getLastAllowance(), BigInteger.valueOf(1000));
		System.out.println("(TEST) pedro's allowance is 1000 ISTCOIN from paulo");

		// now pedro will try to execute a transferFrom tx from paulo's account to joao
		pedro.sendTransferFrom(paulo.getClientName(), joao.getClientName(), BigInteger.valueOf(1000), CoinType.ISTCOIN);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(pedro.getLastTransferReply());
		});
		// finally, check if it went well
		Assertions.assertTrue(pedro.getLastTransferReply().getSuccess());
		System.out.println("(TEST) pedro transfered 1000 ISTCOIN from paulo to joao");

		// -- DEPCOIN TRANSFERS -- //
		// paulo: 100, joao: 100, pedro: 100
		TestUtils.testTransfer(paulo, joao, BigInteger.valueOf(50), BigInteger.valueOf(50),
				BigInteger.valueOf(150), CoinType.DEPCOIN);
		// paulo: 50, joao: 150, pedro: 100
		TestUtils.testTransfer(joao, pedro, BigInteger.valueOf(100),
				BigInteger.valueOf(50), BigInteger.valueOf(200), CoinType.DEPCOIN);
		// paulo: 50, joao: 50, pedro: 200
//		TestUtils.testTransfer(pedro, paulo, BigInteger.valueOf(50),
//				BigInteger.valueOf(150), BigInteger.valueOf(200), CoinType.DEPCOIN);
	}

	@Test
	void testDepCoinTransfers() throws Exception {
		// start client and members
		ExecutorService ex = Executors.newCachedThreadPool();
		this.executor = ex;
		Client paulo = startClient("paulo", CLIENT_PORT, memberInfo, executor);
		Client joao = startClient("joao", CLIENT_PORT + 1, memberInfo, executor);
		Client pedro = startClient("pedro", CLIENT_PORT + 2, memberInfo, executor);
		this.clients = new ArrayList<>(Arrays.asList(paulo, joao, pedro));
		this.members = TestUtils.startHonestMembers(BASE_MEMBER_PORT, memberInfo, clientInfo, executor);

		// paulo: 100, joao: 100, pedro: 100
		TestUtils.testTransfer(paulo, joao, BigInteger.valueOf(50), BigInteger.valueOf(50),
				BigInteger.valueOf(150), CoinType.DEPCOIN);
		// paulo: 50, joao: 150, pedro: 100
		TestUtils.testTransfer(joao, pedro, BigInteger.valueOf(100),
				BigInteger.valueOf(50), BigInteger.valueOf(200), CoinType.DEPCOIN);
		// paulo: 50, joao: 50, pedro: 200
		// TODO -> fix this test
//		TestUtils.testTransfer(pedro, paulo, BigInteger.valueOf(50),
//				BigInteger.valueOf(150), BigInteger.valueOf(100), CoinType.DEPCOIN);
		// pedro --50-> paulo:
		// paulo: 100, joao: 50, pedro: 150
	}

}
