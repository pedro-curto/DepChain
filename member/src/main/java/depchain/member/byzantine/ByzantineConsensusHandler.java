package depchain.member.byzantine;

import depchain.common.DCLogger;
import depchain.common.domain.Block;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Transaction;
import depchain.common.domain.ValueTimestampPair;
import depchain.common.messaging.consensus.*;
import depchain.member.domain.ConsensusHandler;
import depchain.member.domain.ConsensusLeaderState;
import depchain.member.domain.Member;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ByzantineConsensusHandler extends ConsensusHandler {
    private ValueTimestampPair forgedValue;

    public ByzantineConsensusHandler(Member member, DCLogger dcLogger, ConsensusState consensusState) {
		super(member, dcLogger, consensusState);
    }

    @Override
    public boolean collectedPhase() {
        dcLogger.log("[Byzantine] Waiting for states to decide value");
        List<StateMessage> collectedStates = this.consensusState.waitForCollectedMessage();
        if (collectedStates == null || collectedStates.size() < member.getConfig().getByzantineQuorum()) {
            // abort
            dcLogger.log("[Byzantine] Aborted after collecting states");
            return false;
        }
        ValueTimestampPair decidedVTP = decideOnCollectedValues(collectedStates);
        if (decidedVTP == null) {
            // abort
            dcLogger.log("[Byzantine] Aborted after deciding null value");
            return false;
        }

        Block decidedBlock = decidedVTP.getValue();
        Transaction decidedTx = decidedBlock.getTransactions().getFirst();
        Transaction forgedTx = new Transaction(
                decidedTx.getSender(),
                "0x",
                "0x12345678912345678912",
                BigInteger.valueOf(1000),
                decidedTx.getSignature(),
                decidedTx.getNonce(),
                decidedTx.getTransactionType(),
                decidedTx.getCoinType(),
                decidedTx.getClientPort()
        );
        Block forgedBlock = new Block(
                "0x1",
                List.of(forgedTx),
                decidedBlock.getBlockNumber() + 1,
                System.currentTimeMillis()
        );
        forgedValue = new ValueTimestampPair(this.consensusState.getEpoch(), forgedBlock);
        dcLogger.log("[Byzantine] Decided tx: " + decidedTx);
        dcLogger.log("[Byzantine] Decided block: " + decidedBlock);
        dcLogger.log("[Byzantine] Forged tx: " + forgedTx);
        dcLogger.log("[Byzantine] Forged block: " + forgedBlock);
        dcLogger.log("[Byzantine] Forged value: " + forgedValue);
        getConsensusState().updateWriteSet(forgedValue);
        WriteMessage writeMessage = new WriteMessage(forgedValue, member.getConfig().getPort(), getConsensusState().getInstance());
        member.broadCastMessage(writeMessage);
        return true;
    }

    @Override
    public boolean writePhase() {
        dcLogger.log("Waiting for write quorum of size: " + member.getConfig().getByzantineQuorum() + "...");
        ValueTimestampPair writeValts = this.consensusState.waitForWriteQuorum(member.getConfig().getByzantineQuorum());
        if (writeValts == null) {
            // Abort
            dcLogger.log("[Byzantine] ABORTED (WRITE)");
            return false;
        }
        dcLogger.log("[Byzantine] Quorum of WRITE reached");
        //ValueTimestampPair writeValts = new ValueTimestampPair(this.consensusState.getEpoch(), writeValue);
        this.consensusState.setCurrent(forgedValue);
        return true;
    }

    @Override
    public Block acceptPhase() {
        AcceptMessage acceptMessage = new AcceptMessage(forgedValue, member.getConfig().getPort(), consensusState.getInstance());
        dcLogger.log("[Byzantine] Broadcasting: " + acceptMessage);
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
}
