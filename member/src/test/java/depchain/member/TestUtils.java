package depchain.member;

import depchain.client.domain.Client;
import depchain.common.domain.Entity;
import depchain.member.byzantine.*;
import depchain.member.domain.Member;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;

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

	public static Member startByzantineMember(String name, int port, List<Entity> members, List<Entity> clients, String byzantineType, ExecutorService executor) throws Exception {
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
			case "byz-perfect-link":
				byzantineMember = new MemberPerfectLinkByzantine(name, members, clients, port, "localhost", false);
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

	public static Client startClient(String name, int port, List<Entity> members, ExecutorService executor) throws Exception {
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
