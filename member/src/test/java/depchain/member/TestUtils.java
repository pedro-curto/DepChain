package depchain.member;

import depchain.client.domain.Client;
import depchain.common.domain.Entity;
import depchain.member.byzantine.*;
import depchain.member.domain.Member;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class TestUtils {

	public static void sendAppendAndCheck(Client client, String value, List<Member> members) throws Exception {
		client.sendAppend(value);
		Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			for (Member member : members) {
				Assertions.assertTrue(member.getBlockchainState().contains(value),
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
}
