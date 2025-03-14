package depchain.member.byzantine;

import depchain.common.PerfectLink;
import depchain.common.PerfectLinkByzantine;
import depchain.common.domain.Entity;
import depchain.common.messaging.*;
import depchain.member.domain.Member;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.util.List;

public class MemberPerfectLinkByzantine extends Member {
	public MemberPerfectLinkByzantine(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
		super(memberName, members, clients, port, address, debug);
		System.out.println("MemberPerfectLinkByzantine started at port " + port);
	}

	@Override
	public void createPerfectLink(DatagramSocket serverSocket, KeyPair myKeyPair, List<Entity> entities, boolean debug) {
		this.perfectLink = new PerfectLinkByzantine(serverSocket, messageQueue, myKeyPair, entities, debug);
	}
}
