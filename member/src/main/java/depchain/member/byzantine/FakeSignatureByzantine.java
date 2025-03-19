package depchain.member.byzantine;

import depchain.common.Security;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.messaging.ReadMessage;
import depchain.common.messaging.StateMessage;
import depchain.member.domain.Member;

import java.util.List;
import java.util.Random;

public class FakeSignatureByzantine extends Member {

    private static final Random random = new Random();

    public FakeSignatureByzantine(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
        super(memberName, members, clients, port, address, debug);
        System.out.println("FakeSignatureByzantine started at port " + port);
    }

    public String getRandomMemberName() {
        String memberName;
        do {
            memberName = members.get(random.nextInt(members.size())).getEntityName();
        } while(memberName.equals(myName));

        return memberName;
    }

    public int getMemberPort(String name) {
        for (Entity entity : members) {
            if (entity.getEntityName().equals(name)) {
                return entity.getPort();
            }
        }
        dcLogger.error("COULDN'T FIND MEMBER");
        return -1;
    }

    @Override
    public void handleRead(ReadMessage readMessage) {
        dcLogger.log("Received: " + readMessage);
        String dataToSign = consensusState.getCurrent().toString() + consensusState.getWriteset();
        String mySignature = Security.makeDS(dataToSign, Security.getMyPrivateKey(myName));

        String randomMemberName = getRandomMemberName();
        // Send as a another member name
        ConsensusState myState = new ConsensusState(randomMemberName, consensusState.getCurrent(), consensusState.getWriteset());
        StateMessage stateMessage = new StateMessage(myState, mySignature, consensusState.getInstance(), getMemberPort(randomMemberName));
        dcLogger.log("Sending fake state message... : " + stateMessage);
        sendToLeader(stateMessage);
    }
}
