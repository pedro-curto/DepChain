package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.PerfectLinkByzantine;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.messaging.*;
import depchain.member.domain.Config;
import depchain.member.domain.Member;
import depchain.member.state.BlockchainState;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MemberPerfectLinkByzantine extends Member {

	public MemberPerfectLinkByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, BlockchainState bcState, BlockingQueue<Message> messageQueue, BlockingQueue<AppendMessage> appendQueue) {
		super(config, dcLogger, pf, cState, bcState, messageQueue, appendQueue);
	}

	// TODO -> this has to set the perfect link of the object and not override anything
	//@Override
	public void createPerfectLink(DatagramSocket serverSocket, KeyPair myKeyPair, List<Entity> entities, boolean debug) {
		this.perfectLink = new PerfectLinkByzantine(serverSocket, messageQueue, myKeyPair, entities, debug);
	}
}
