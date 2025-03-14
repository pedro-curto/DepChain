package depchain.member.byzantine;

import depchain.common.domain.Entity;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.AcceptMessage;
import depchain.common.messaging.ClientReplyMessage;
import depchain.common.messaging.StateMessage;
import depchain.common.messaging.WriteMessage;
import depchain.member.domain.Member;

import java.util.List;

public class WrongWriteAcceptByzantine extends Member {

    // Sends Write and Accept messages with the same byzantine value every time

    public WrongWriteAcceptByzantine(String memberName, List<Entity> members, List<Entity> clients, int port, String address, boolean debug) {
        super(memberName, members, clients, port, address, debug);
        System.out.println("WrongWriteAcceptByzantine started at port " + port);
    }

    // Byzantines send the same WriteMessage
    @Override
    public boolean decideValue() {
        dcLogger.log("Waiting for states to decide value");
        List<StateMessage> collectedStates = this.consensusState.waitForCollectedMessage();
        if (collectedStates == null || collectedStates.size() < this.byzantineQuorum) {
            // abort
            dcLogger.log("aborted after collecting states");
            this.consensusState.nextEpoch();
            return false;
        }
        String value = decideOnCollectedValues(collectedStates);
        if (value == null) {
            // abort
            dcLogger.log("aborted after deciding null value");
            this.consensusState.nextEpoch();
            return false;
        }
        // Creates byzantine write, neglects decided value
        ValueTimestampPair byzantinePair = new ValueTimestampPair(this.consensusState.getEpoch(), "Byzantine");
        this.consensusState.updateWriteSet(byzantinePair);
        WriteMessage writeMessage = new WriteMessage(byzantinePair, this.port, consensusState.getInstance());
        dcLogger.log("Broadcasting: " + writeMessage);
        broadCastMessage(writeMessage);
        return writePhase();
    }

    // Byzantines send the same AcceptMessage
    @Override
    public boolean writePhase() {
        dcLogger.log("Waiting for write quorum of size: " + byzantineQuorum + "...");
        String writeValue = this.consensusState.waitForWriteQuorum(this.byzantineQuorum);
        if (writeValue == null) {
            // Abort
            dcLogger.log("ABORTED (WRITE)");
            this.consensusState.nextEpoch();
            return false;
        }
        dcLogger.log("Quorum of WRITE reached");
        ValueTimestampPair writeValts = new ValueTimestampPair(this.consensusState.getEpoch(), "Byzantine");
        this.consensusState.setCurrent(writeValts);

        // Broadcast ACCEPT and wait for quorum to DECIDE value
        AcceptMessage acceptMessage = new AcceptMessage("Byzantine", this.port, consensusState.getInstance());
        dcLogger.log("Broadcasting: " + acceptMessage);
        broadCastMessage(acceptMessage);

        dcLogger.log("Waiting for accept quorum of size: " + byzantineQuorum + "...");
        String accept = this.consensusState.waitForAcceptQuorum(this.byzantineQuorum);
        if (accept == null) {
            // Abort
            dcLogger.log("ABORTED (ACCEPT)");
            this.consensusState.nextEpoch();
            return false;
        }
        dcLogger.log("Quorum of accept reached");

        // DECIDE value
        this.blockchainState.appendString(consensusState.getCurrent().getValue());
        if (isLeader()) {
            ClientReplyMessage clientReplyMessage = new ClientReplyMessage(accept,true, consensusState.getInstance());
            broadCastToClients(clientReplyMessage);
        }
        this.consensusState.nextInstance();
        return true;
    }
}
