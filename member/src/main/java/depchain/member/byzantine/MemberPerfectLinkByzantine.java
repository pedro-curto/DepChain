package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.domain.ConsensusState;
import depchain.common.messaging.*;
import depchain.common.messaging.library.AppendMessage;
import depchain.member.domain.Config;
import depchain.member.domain.Member;
import depchain.member.state.StringChain;

import java.util.concurrent.BlockingQueue;

public class MemberPerfectLinkByzantine extends Member {
	public MemberPerfectLinkByzantine(Config config, DCLogger dcLogger, PerfectLink pf, ConsensusState cState, StringChain bcState, BlockingQueue<Message> messageQueue) {
		super(config, dcLogger, pf, cState, bcState, messageQueue);
	}
}
