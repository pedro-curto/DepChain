package depchain.member.domain;

import com.google.gson.JsonObject;
import depchain.common.DCLogger;
import depchain.common.domain.*;
import depchain.common.messaging.ClientReplyMessage;
import depchain.common.messaging.MessageType;
import depchain.common.messaging.consensus.*;
import depchain.common.messaging.library.AppendMessage;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConsensusHandler {

    private Member member;
    private DCLogger dcLogger;
    private ConsensusState consensusState;
    private BlockingQueue<ValueTimestampPair> consensusQueue = new LinkedBlockingQueue<>();

    public ConsensusHandler(Member member, DCLogger dcLogger, ConsensusState consensusState) {
        this.member = member;
        this.dcLogger = dcLogger;
        this.consensusState = consensusState;
    }

    public ConsensusState getConsensusState() {
        return consensusState;
    }

    // only leader runs starts consensus
    public boolean startConsenusLeader(ConsensusLeaderState leaderState) {
        // TODO on member, dont forget the things before read phase (noncecheck)
        // TODO leaderState.setCurrent(valts); set new value of consensus
        dcLogger.log("-- STARTING CONSENSUS FOR '" + leaderState.getCurrent() + "' --");
        readPhase(leaderState);
        statePhase(leaderState);
        // the rest of the consensus is the same for member and leader
        return startConsensusMember();
    }

    public boolean startConsensusMember() {
        if(!collectedPhase()) {
            // aborted
            return false;
        }
        if (!writePhase()) {
            // aborted
            return false;
        }
        Block acceptedValue = acceptPhase();
        if(acceptedValue == null) {
            // aborted
            return false;
        }
        decide(acceptedValue);
        return true;
    }

    public void addValtsForConsensus(ValueTimestampPair valts) {
        try {
            consensusQueue.put(valts);
        } catch (InterruptedException e) {
            dcLogger.error("Error while adding object to consensusQueue: " + e.getMessage());
        }
    }

    public ValueTimestampPair getNextValtsForConsensus() {
        try {
            dcLogger.log("[ME] Waiting to take...");
            ValueTimestampPair valt = consensusQueue.take();
            dcLogger.log("[ME] Took");
            return valt;
        }  catch (InterruptedException e) {
            dcLogger.error("Error while taking object from consensusQueue: " + e.getMessage());
        }
        return null;
    }

    public void readPhase(ConsensusLeaderState leaderState) {
        ReadMessage readMessage = new ReadMessage(leaderState.getInstance());
        dcLogger.log("Broadcasting: " + readMessage);
        member.broadCastMessage(readMessage);
    }

    public void statePhase(ConsensusLeaderState leaderState) {
        dcLogger.log("Waiting for state quorum of size: " + member.getConfig().getByzantineQuorum() + "...");
        List<StateMessage> states = leaderState.waitForStateQuorum();
        if (leaderState.getCaughtInvalidSignature()) {
            dcLogger.log("Caught invalid signature in state quorum");
            member.setCaughtInvalidSignature();
        }
        dcLogger.log("Quorum of STATE reached");

        // Send the collection of states to all the members
        CollectedMessage collectedMessage = new CollectedMessage(states, member.getConfig().getPort(), leaderState.getInstance());
        dcLogger.log("Broadcasting: " + collectedMessage);
        member.broadCastMessage(collectedMessage);
    }

    // old decideValue()
    public boolean collectedPhase() {
        dcLogger.log("Waiting for states to decide value");
        List<StateMessage> collectedStates = this.consensusState.waitForCollectedMessage();
        if (collectedStates == null || collectedStates.size() < member.getConfig().getByzantineQuorum()) {
            // abort
            dcLogger.log("Aborted after collecting states");
            return false;
        }
        ValueTimestampPair decidedVTP = decideOnCollectedValues(collectedStates);
        if (decidedVTP == null) {
            // abort
            dcLogger.log("Aborted after deciding null value");
            return false;
        }
        //ValueTimestampPair decidePair = new ValueTimestampPair(this.consensusState.getEpoch(), value);
        this.consensusState.updateWriteSet(decidedVTP);
        WriteMessage writeMessage = new WriteMessage(
                decidedVTP,
                member.getConfig().getPort(),
                consensusState.getInstance());
        dcLogger.log("Broadcasting: " + writeMessage);
        member.broadCastMessage(writeMessage);
        return true;
    }

    public boolean writePhase() {
        dcLogger.log("Waiting for write quorum of size: " + member.getConfig().getByzantineQuorum() + "...");
        ValueTimestampPair writeValts = this.consensusState.waitForWriteQuorum(member.getConfig().getByzantineQuorum());
        if (writeValts == null) {
            // Abort
            dcLogger.log("ABORTED (WRITE)");
            return false;
        }
        dcLogger.log("Quorum of WRITE reached");
        //ValueTimestampPair writeValts = new ValueTimestampPair(this.consensusState.getEpoch(), writeValue);
        this.consensusState.setCurrent(writeValts);
        return true;
    }

    public Block acceptPhase() {
        AcceptMessage acceptMessage = new AcceptMessage(consensusState.getCurrent(), member.getConfig().getPort(), consensusState.getInstance());
        dcLogger.log("Broadcasting: " + acceptMessage);
        member.broadCastMessage(acceptMessage);

        dcLogger.log("Waiting for accept quorum of size: " + member.getConfig().getByzantineQuorum() + "...");
        Block acceptedValue = this.consensusState.waitForAcceptQuorum(member.getConfig().getByzantineQuorum());
        if (acceptedValue == null) {
            // Abort
            dcLogger.log("ABORTED (ACCEPT)");
            return null;
        }
        dcLogger.log("Quorum of accept reached");
        return acceptedValue;
    }

    public void decide(Block acceptedBlock) {
        dcLogger.verbose("Block decided: " + acceptedBlock);
        member.executeTransactions(acceptedBlock);
        member.setLastBlock(acceptedBlock);
        member.replyTransactions(acceptedBlock.getTransactions());
    }

    public ValueTimestampPair decideOnCollectedValues(List<StateMessage> collectedStates) {
        ValueTimestampPair leaderValue = null;
        ValueTimestampPair highest = new ValueTimestampPair(-1, null, -1, -1);

        for (StateMessage thisState : collectedStates) {
            // TODO uncomment
            if (!member.verifyMemberStateAuthenticity(thisState)) {
                dcLogger.error("Signature is invalid for " + thisState.getState().getMemberName());
                // TODO -> hardcoded for the test (fix)
                member.setCaughtInvalidSignature();
                continue;
            } else {
                dcLogger.log("Signature is valid for " + thisState.getState().getMemberName());
            }
            if (thisState.getConsensusInstance() != this.consensusState.getInstance()) {
                dcLogger.error("State message of different instance among COLLECTED");
                continue;
            }

            ValueTimestampPair vts = thisState.getState().getCurrent();
            // sets vts clientName to name that comes in the consensus state inside state message
            //vts.setClientName(thisState.getState().getMemberName());
            if (highest.getTimestamp() > vts.getTimestamp()) {
                continue;
            }
            if (thisState.getState().getMemberName().equalsIgnoreCase(member.getConfig().getLeader().getEntityName())) {
                leaderValue = vts;
            }

            // count in how many writesets it appears
            // must be at least 2f +1
            int count = 0;
            for (StateMessage otherState : collectedStates) {
                for (ValueTimestampPair pair : otherState.getState().getWriteset()) {
                    if (pair.getValue().equals(vts.getValue()) && pair.getTimestamp() >= vts.getTimestamp()) {
                        count++;
                        break;
                    }
                }
                if (count >= member.getConfig().getByzantineQuorum()) {
                    highest = vts;
                    break;
                }
            }
        }

        if (highest.getValue() != null) {
            return highest;
        }
        // default to leader value
        if (leaderValue == null || leaderValue.getValue() == null) {
            dcLogger.error("Did not get leader value in COLLECTED message");
            return null;
        }
        // TODO -> how to check client signatures for blocks? check for each field?
//        if (!member.checkClientSignature(leaderValue)) {
//            dcLogger.error("Leader forged new value");
//            return null;
//        }
            // check if each tx in block is valid
//            List<Transaction> txs = JsonAdapter.parseTransactions(leaderValue.getValue());
//            for (Transaction tx : txs) {
//                if (!Security.validateTransaction(tx)) {
//                    dcLogger.error("Signature for transaction is invalid: " + tx);
//                    return null;
//                }
//            }

//        else {
//            // append request (stringchain)
//            if (!checkClientSignature(leaderValue)) {
//                dcLogger.error("Leader forged new value");
//                return null;
//            }
//        }
        return leaderValue;
    }
}
