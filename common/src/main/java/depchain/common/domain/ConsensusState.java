package depchain.common.domain;

import depchain.common.messaging.AcceptMessage;
import depchain.common.messaging.Message;
import depchain.common.messaging.StateMessage;
import depchain.common.messaging.WriteMessage;

import java.util.*;

public class ConsensusState {

    private String memberName;
    private ValueTimestampPair current;
    private List<ValueTimestampPair> writeset;
    private int currentConsensusInstance;
    public final Object lock;
    private Map<Integer, WriteMessage> writeMessages;
    private Map<Integer, AcceptMessage> acceptMessages;
    private Map<String, Integer> writeCounters = new HashMap<>();
    private Map<String, Integer> acceptCounters = new HashMap<>();
    private int epoch = 0;

    public ConsensusState(String memberName, int currentConsensusInstance) {
        // Initial State
        this.memberName = memberName;
        // TODO -> rever valor com que se inicializa o current
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
    public int getCurrentConsensusInstance() {
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
        return "ConsensusState{" +
                "memberName='" + memberName + '\'' +
                ", current=" + current +
                ", writeset=" + writeset +
                '}';
    }

    private boolean reachedQuorum(float quorum, Collection<Integer> counters) {
        for (Integer counter : counters) {
            if (counter > quorum) {
                System.out.println("Reached quorum! with " + counter + "messages");
                return true;
            }
        }
        return false;
    }


    private void printValues(Map<String, Integer> values) {
        System.out.print("[ConsensusState] Values: ");
        for (String value : values.keySet()) {
            System.out.print(values.get(value) + "x <" + value + ">, ");
        }
        System.out.println();
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
        }
        return maxValue;
    }


    public String waitForWriteQuorum(float byzantineQuorum) {
        synchronized (lock) {
            while (!reachedQuorum(byzantineQuorum, writeCounters.values())) {
                try {
                    System.out.println("[ConsensusState] Received Write messages: " + writeMessages.size());
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for quorum");
                }
            }
        }
        // Quorum received
        printValues(writeCounters);
        return decideValue(writeCounters);
    }

    public String waitForAcceptQuorum(float byzantineQuorum) {
        synchronized (lock) {
            while (!reachedQuorum(byzantineQuorum, acceptCounters.values())) {
                try {
                    System.out.println("[ConsensusState] Received Accept Messages: " + acceptMessages.size());
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for quorum");
                }
            }
        }
        printValues(acceptCounters);
        return decideValue(acceptCounters);
    }
}
