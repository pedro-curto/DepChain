package depchain.common.domain;

import depchain.common.messaging.AcceptMessage;
import depchain.common.messaging.StateMessage;
import depchain.common.messaging.WriteMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsensusState {

    private String memberName;
    private ValueTimestampPair current;
    private List<ValueTimestampPair> writeset;
    private int currentConsensusInstance;
    private final Object lock = new Object();
    private Map<Integer, WriteMessage> writeMessages;
    private Map<Integer, AcceptMessage> acceptMessages;
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
    }

    public ConsensusState(String memberName, ValueTimestampPair current, List<ValueTimestampPair> writeset) {
        this.memberName = memberName;
        this.current = current;
        this.writeset = writeset;
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

    public void addWritesetEntry(ValueTimestampPair entry) {
        this.writeset.add(entry);
    }

    public boolean isInitialState() {
        return this.current == null;
    }

    public String getDataToSignState() {
        return this.current.toString() + this.writeset.toString();
    }

    public void addWriteMessage(WriteMessage writeMessage) {
        synchronized (lock) {
            // TODO: Handle what happens if messages have higher epoch
            if(writeMessage.getValts().getTimestamp() == epoch &&
                    !writeMessages.containsKey(writeMessage.getPort())) {
                // Write message not received yet
                writeMessages.put(writeMessage.getPort(), writeMessage);
            }
            lock.notifyAll();
        }
    }

    public void addAcceptMessage(AcceptMessage acceptMessage) {
        synchronized (lock) {
            if(!acceptMessages.containsKey(acceptMessage.getPort())) {
                // Accept message not received yet
                acceptMessages.put(acceptMessage.getPort(), acceptMessage);
            }
            lock.notifyAll();
        }
    }

    public int getEpoch() {
        return epoch;
    }
    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    public void nextInstance() {
        this.current = new ValueTimestampPair(0, "");
        this.writeset = new ArrayList<>();
        this.currentConsensusInstance = this.currentConsensusInstance +1;
        this.writeMessages = new HashMap<>();
        this.acceptMessages = new HashMap<>();
    }

    public void nextEpoch() {
        this.writeMessages = new HashMap<>();
        this.acceptMessages = new HashMap<>();
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

    private ValueTimestampPair decideWriteValue() {
        HashMap<ValueTimestampPair, Integer> pairMap = new HashMap<>();
        ValueTimestampPair maxValts = null;
        int maxCount = 0;

        for (WriteMessage writeMessage : writeMessages.values()) {
            ValueTimestampPair valts = writeMessage.getValts();
            pairMap.put(valts, pairMap.getOrDefault(valts, 0) + 1);
            if (pairMap.get(valts) > maxCount) {
                maxCount = pairMap.get(valts);
                maxValts = valts;
            }
        }
        return maxValts;
    }

    private String decideAcceptValue() {
        HashMap<String, Integer> pairMap = new HashMap<>();
        String maxValue = "";
        int maxCount = 0;

        for (AcceptMessage acceptMessage : acceptMessages.values()) {
            String value = acceptMessage.getValue();
            pairMap.put(value, pairMap.getOrDefault(value, 0) + 1);
            if (pairMap.get(value) > maxCount) {
                maxCount = pairMap.get(value);
                maxValue = value;
            }
        }
        return maxValue;
    }

    public ValueTimestampPair waitForWriteQuorum(float byzantineQuorum) {
        synchronized (lock) {
            while (writeMessages.size() < byzantineQuorum) {
                try {
                    System.out.println("Quorum of writes not reached yet. Current size: " + writeset.size());
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for quorum");
                }
            }
        }
        // Quorum received
        return decideWriteValue();
    }

    public String waitForAcceptQuorum(float byzantineQuorum) {
        synchronized (lock) {
            while (acceptMessages.size() <= byzantineQuorum) {
                try {
                    System.out.println("Quorum of accepts not reached yet. Current size: " + writeset.size());
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for quorum");
                }
            }
        }
        // Quorum received
        return decideAcceptValue();
    }
}
