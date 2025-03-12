package depchain.common.domain;

import depchain.common.messaging.*;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class ConsensusState {

    private static final long TIMEOUT = 5000; // 5 seconds

    private final String memberName;
    private ValueTimestampPair current;
    private List<ValueTimestampPair> writeset;
    private int currentConsensusInstance;
    public final Object lock;
    private Map<Integer, WriteMessage> writeMessages;
    private Map<Integer, AcceptMessage> acceptMessages;
    private Map<String, Integer> writeCounters = new HashMap<>();
    private Map<String, Integer> acceptCounters = new HashMap<>();
    private CollectedMessage collectedMessage = null;
    private int epoch = 0;

    public ConsensusState(String memberName, int currentConsensusInstance) {
        // Initial State
        this.memberName = memberName;
        this.current = new ValueTimestampPair(0, "");
        this.writeset = new ArrayList<>();
        this.currentConsensusInstance = currentConsensusInstance;
        this.writeMessages = new HashMap<>();
        this.acceptMessages = new HashMap<>();
        this.lock = new Object();
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
    public void setCurrent(ValueTimestampPair current) {
        this.current = current;
    }
    public int getEpoch() {
        return epoch;
    }
    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    public void addWriteMessage(WriteMessage writeMessage) {
        synchronized (lock) {
            if(writeMessage.getValts().getTimestamp() == epoch &&
                    !writeMessages.containsKey(writeMessage.getPort())) {
                // Write message not received yet
                writeMessages.put(writeMessage.getPort(), writeMessage);

                String value = writeMessage.getValts().getValue();
                writeCounters.put(value, writeCounters.getOrDefault(value, 0) + 1);
            }
            lock.notifyAll();
        }
    }

    public void addAcceptMessage(AcceptMessage acceptMessage) {
        synchronized (lock) {
            if(!acceptMessages.containsKey(acceptMessage.getPort())) {
                // Accept message not received yet
                acceptMessages.put(acceptMessage.getPort(), acceptMessage);

                String value = acceptMessage.getValue();
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

    private boolean reachedQuorum(float quorum, Collection<Integer> counters) {
        System.out.print("REACHED QUORUM??: ");
        printValues(writeCounters);
        printValues(acceptCounters);
        System.out.println();
        for (Integer counter : counters) {
            if (counter > quorum) {
                return true;
            }
        }
        return false;
    }

    private void printValues(Map<String, Integer> values) {
        for (String value : values.keySet()) {
            System.out.print(values.get(value) + "x <" + value + ">, ");
        }
    }

    private String decideValue(Map<String, Integer> values) {
        int maxCounter = 0;
        String maxValue = null;
        for (String value : values.keySet()) {
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

    public String waitForWriteQuorum(float byzantineQuorum) {
        System.out.println("Reached waiting for write quorum");
        System.out.println("Timeout: " + TIMEOUT);
        if(waitForQuorum(byzantineQuorum, writeCounters, TIMEOUT)) {
            // Quorum reached
            System.out.print("[ConsensusState] Write values: ");
            printValues(writeCounters);
            System.out.println();
            return decideValue(writeCounters);
        }
        // Timeout
        System.out.println("[ConsensusState] waiting for write timed out ");
        return null;
    }

    public String waitForAcceptQuorum(float byzantineQuorum) {
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

    public boolean waitForQuorum(float byzantineQuorum, Map<String, Integer> counter, long timeoutMillis) {
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
