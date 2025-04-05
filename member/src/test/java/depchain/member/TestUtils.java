package depchain.member;

import depchain.client.domain.ByzantineClient;
import depchain.client.domain.Client;
import depchain.common.domain.Entity;
import depchain.common.messaging.CoinType;
import depchain.member.domain.Member;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class TestUtils {

	public static void sendAppendAndCheck(Client client, String value, List<Member> members) throws Exception {
		client.sendAppend(value);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			for (Member member : members) {
				Assertions.assertTrue(member.getStringChain().contains(value),
						member.getName() + " should have the value");
			}
		});
	}

	public static Member startMember(String name, int port, List<Entity> members, List<Entity> clients, ExecutorService executor) throws Exception {
		Member member = MemberBuilder.testBuild(name, new ArrayList<>(members), new ArrayList<>(clients), port, "localhost", "normal");
		executor.submit(() -> {
			try {
				member.start();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return member;
	}

	public static Member startByzantineMember(String name, int port, List<Entity> members, List<Entity> clients, String byzantineType, ExecutorService executor) throws Exception {
		Member byzantineMember = MemberBuilder.testBuild(name, new ArrayList<>(members), new ArrayList<>(clients), port, "localhost", byzantineType);
		executor.submit(() -> {
			try {
				byzantineMember.start();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return byzantineMember;
	}

	public static Client startClient(String name, int port, List<Entity> members, ExecutorService executor) throws Exception {
		Client client = new Client(name, port, new ArrayList<>(members), true);
		executor.submit(() -> {
			try {
				client.start();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return client;
	}

	public static ByzantineClient startByzantineClient(String name, int port, List<Entity> members, int byzantineType, ExecutorService executor) throws Exception {
		ByzantineClient client = new ByzantineClient(name, port, new ArrayList<>(members), byzantineType, true);
		executor.submit(() -> {
			try {
				client.start();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return client;
	}

	public static List<Member> startHonestMembers(int basePort, List<Entity> memberInfo,
												  List<Entity> clientInfo, ExecutorService executor) throws Exception {
		Member leader = startMember("pedroribeiro", basePort, memberInfo, clientInfo, executor);
		Member honest1 = startMember("pedrocurto", basePort + 1, memberInfo, clientInfo, executor);
		Member honest2 = startMember("rodrigogreedy", basePort + 2, memberInfo, clientInfo, executor);
		Member honest3 = startMember("dybizantino", basePort + 3, memberInfo, clientInfo, executor);
		Thread.sleep(5000);
		return new ArrayList<>(Arrays.asList(leader, honest1, honest2, honest3));
	}


	public static void testTransfer(
			Client fromClient,
			Client toClient,
			BigInteger amount,
			BigInteger expectedFromBalance,
			BigInteger expectedToBalance,
			CoinType coinType
	) {
		// send transfer
		String toName = toClient.getClientName();
		String fromName = fromClient.getClientName();
		fromClient.sendTransfer(toName, amount, coinType);

		// wait for the reply to appear
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(fromClient.getLastTransferReply());
		});
		// verify transfer was successful
		Assertions.assertTrue(fromClient.getLastTransferReply().getSuccess());
		fromClient.setLastTransferReply(null);

		// check updated balances
		fromClient.sendGetBalance(fromName, coinType);
		toClient.sendGetBalance(toName, coinType);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(fromClient.getLastBalance());
			Assertions.assertNotNull(toClient.getLastBalance());
		});

		// compare expected balances
		Assertions.assertEquals(expectedFromBalance, fromClient.getLastBalance());
		Assertions.assertEquals(expectedToBalance, toClient.getLastBalance());
		// reset last balance
		fromClient.setLastBalance(null);
		toClient.setLastBalance(null);
	}

	public static void testAllowanceMechanism(
			Client ownerClient,
			Client spenderClient,
			Client toClient,
			BigInteger amount,
			BigInteger expectedOwnerBalance,
			BigInteger expectedToBalance,
			CoinType coinType
	) {
		String toName = toClient.getClientName();
		String spenderName = spenderClient.getClientName();
		String ownerName = ownerClient.getClientName();

		// owner approves spender to spend <amount> ISTCOIN
		ownerClient.sendApprove(spenderName, amount, coinType);
		// checks op success
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(ownerClient.getLastTransferReply());
		});
		Assertions.assertTrue(ownerClient.getLastTransferReply().getSuccess());
		System.out.println("(TEST) owner approved sender to spend <amount> ISTCOIN");
		ownerClient.setLastTransferReply(null);
		// we check spender's allowance
		spenderClient.sendGetAllowance(ownerName, spenderName, CoinType.ISTCOIN);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(spenderClient.getLastAllowance());
		});
		Assertions.assertEquals(spenderClient.getLastAllowance(), amount);
		System.out.println("(TEST) spender's allowance is <amount> ISTCOIN from paulo");

		// now pedro will try to execute a transferFrom tx from paulo's account to joao
		spenderClient.sendTransferFrom(ownerName, toName, amount, CoinType.ISTCOIN);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(spenderClient.getLastTransferReply());
		});
		// finally, check if it went well
		Assertions.assertTrue(spenderClient.getLastTransferReply().getSuccess());
		System.out.println("(TEST) spender transfered <amount> ISTCOIN from owner to recipient");
		// check updated balances
		ownerClient.sendGetBalance(ownerName, coinType);
		toClient.sendGetBalance(toName, coinType);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Assertions.assertNotNull(ownerClient.getLastBalance());
			Assertions.assertNotNull(toClient.getLastBalance());
		});

		// compare expected balances
		Assertions.assertEquals(expectedOwnerBalance, ownerClient.getLastBalance());
		Assertions.assertEquals(expectedToBalance, toClient.getLastBalance());
		// reset last balance
		ownerClient.setLastBalance(null);
		toClient.setLastBalance(null);
	}
}
