package depchain.common.domain;

import depchain.common.DCLogger;
import depchain.common.messaging.consensus.AcceptMessage;
import depchain.common.messaging.consensus.CollectedMessage;
import depchain.common.messaging.consensus.StateMessage;
import depchain.common.messaging.consensus.WriteMessage;

import java.util.*;

public class ConsensusState {

    protected static final long TIMEOUT = 5000; // 5 seconds
    private final String memberName;
    private ValueTimestampPair current;
    private List<ValueTimestampPair> writeset;
    private int currentConsensusInstance;
    public final Object lock;
    private Map<Integer, WriteMessage> writeMessages;
    private Map<Integer, AcceptMessage> acceptMessages;
    private Map<ValueTimestampPair, Integer> writeCounters = new HashMap<>();
    private Map<ValueTimestampPair, Integer> acceptCounters = new HashMap<>();
    private CollectedMessage collectedMessage = null;
    private int epoch = 0;
    private DCLogger logger;

    public ConsensusState(String memberName, int currentConsensusInstance) {
        // Initial State
        this.memberName = memberName;
        this.current = new ValueTimestampPair(0, "");
        this.writeset = new ArrayList<>();
        this.currentConsensusInstance = currentConsensusInstance;
        this.writeMessages = new HashMap<>();
        this.acceptMessages = new HashMap<>();
        this.lock = new Object();
        this.logger = new DCLogger(ConsensusState.class, false);
    }

    public ConsensusState(String memberName, ValueTimestampPair current, List<ValueTimestampPair> writeset) {
        this.memberName = memberName;
        this.current = current;
        this.writeset = writeset;
        this.lock = new Object();
    }

    public String getMemberName() {
        return memberName;
    }
    public ValueTimestampPair getCurrent() {
        return current;
    }
    public List<ValueTimestampPair> getWriteset() {
        return writeset;
    }
    public int getInstance() {
        return currentConsensusInstance;
    }
    public void setInstance(int instance) {
        this.currentConsensusInstance = instance;
    }
    public void setCurrent(ValueTimestampPair current) {
        this.current = current;
    }
    public int getEpoch() {
        return epoch;
    }

    public void updateWriteSet(ValueTimestampPair decidedPair) {
        for (ValueTimestampPair pair : this.writeset) {
            if (pair.getValue().equals(decidedPair.getValue())) {
                pair.setTimestamp(decidedPair.getTimestamp());
                return;
            }
        }
    }

    public void addWriteMessage(WriteMessage writeMessage) {
        synchronized (lock) {
            if(writeMessage.getValts().getTimestamp() == epoch &&
                    !writeMessages.containsKey(writeMessage.getPort())) {
                // Write message not received yet
                writeMessages.put(writeMessage.getPort(), writeMessage);

                ValueTimestampPair valts = writeMessage.getValts();
                writeCounters.put(valts, writeCounters.getOrDefault(valts, 0) + 1);
            }
            lock.notifyAll();
        }
    }

    public void addAcceptMessage(AcceptMessage acceptMessage) {
        synchronized (lock) {
            if(!acceptMessages.containsKey(acceptMessage.getPort())) {
                // Accept message not received yet
                acceptMessages.put(acceptMessage.getPort(), acceptMessage);

                ValueTimestampPair value = acceptMessage.getValts();
                acceptCounters.put(value, acceptCounters.getOrDefault(value, 0) + 1);
            }
            lock.notifyAll();
        }
    }

    // add to do it this way because gson was not allowing a queue
    public void addCollectedMessage(CollectedMessage collectedMessage) {
        synchronized (lock) {
            this.collectedMessage = collectedMessage;
            lock.notifyAll();
        }
    }

    private boolean reachedQuorum(int quorum, Collection<Integer> counters) {
        //System.out.print("Quorum I want is " + quorum + " and I have " + counters + " ");
        //System.out.println("Write counters: " + writeCounters);
        //printValues(writeCounters);
        //printValues(acceptCounters);
        //System.out.println();
        for (Integer counter : counters) {
            if (counter >= quorum) {
                return true;
            }
        }
        return false;
    }

    private void printValues(Map<ValueTimestampPair, Integer> values) {
        for (ValueTimestampPair value : values.keySet()) {
            System.out.print(values.get(value) + "x <" + value + ">, ");
        }
    }

    private ValueTimestampPair decideValue(Map<ValueTimestampPair, Integer> values) {
        int maxCounter = 0;
        ValueTimestampPair maxValue = null;
        for (ValueTimestampPair value : values.keySet()) {
            if (values.get(value) > maxCounter) {
                maxCounter = values.get(value);
                maxValue = value;
            }
        }
        if (maxValue == null) {
            // Should never happen
            System.out.println("[ERROR] Decided value is null");
        } else {
            System.out.println("[ConsensusState] Decided value: " + maxValue);
        }
        return maxValue;
    }

    // TODO-> add parameter for timeout because of lider, other dont have timeout
    public List<StateMessage> waitForCollectedMessage() {
        synchronized (lock) {
            while (collectedMessage == null) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for quorum");
                    return null;
                }
            }
        }
        List<StateMessage> copy = new ArrayList<>(this.collectedMessage.getStates());
        this.collectedMessage = null;
        return copy;
    }

    public ValueTimestampPair waitForWriteQuorum(int byzantineQuorum) {
        logger.log("Reached waiting for write quorum");
        logger.log("Timeout: " + TIMEOUT);
        if(waitForQuorum(byzantineQuorum, writeCounters, TIMEOUT)) {
            // Quorum reached
            logger.log("[ConsensusState] Write values: ");
            printValues(writeCounters);
            System.out.println();
            return decideValue(writeCounters);
        }
        // Timeout
        logger.log("[ConsensusState] waiting for write timed out ");
        return null;
    }

    public ValueTimestampPair waitForAcceptQuorum(int byzantineQuorum) {
        if(waitForQuorum(byzantineQuorum, acceptCounters, TIMEOUT)) {
            // Quorum reached
            System.out.print("[ConsensusState] Accepted values: ");
            printValues(acceptCounters);
            System.out.println();
            return decideValue(acceptCounters);
        }
        // Timeout
        return null;
    }

    public boolean waitForQuorum(int byzantineQuorum, Map<ValueTimestampPair, Integer> counter, long timeoutMillis) {
        synchronized (lock) {

            System.out.println("reached waiting for quorum");

            long startTime = System.currentTimeMillis();
            long remainingTime = timeoutMillis;

            while (!reachedQuorum(byzantineQuorum, counter.values())) {
                if (remainingTime <= 0) {
                    // Timeout elapsed, quorum not reached
                    return false;
                }

                try {
                    lock.wait(remainingTime);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for quorum");
                    return false;
                }

                // Update remaining time
                long elapsedTime = System.currentTimeMillis() - startTime;
                remainingTime = timeoutMillis - elapsedTime;
            }
        }
        return true;
    }

    public void nextInstance() {
        this.current = new ValueTimestampPair(0, "");
        this.writeset = new ArrayList<>();
        this.currentConsensusInstance = this.currentConsensusInstance +1;
        this.writeMessages = new HashMap<>();
        this.acceptMessages = new HashMap<>();
        this.writeCounters = new HashMap<>();
        this.acceptCounters = new HashMap<>();
        this.epoch = 0;
    }

    public void nextEpoch() {
        this.writeMessages = new HashMap<>();
        this.acceptMessages = new HashMap<>();
        this.writeCounters = new HashMap<>();
        this.acceptCounters = new HashMap<>();
        this.epoch++;
    }

    @Override
    public String toString() {
        return memberName + ": [" + current + ", {" + writeset + "}]";
    }
}
