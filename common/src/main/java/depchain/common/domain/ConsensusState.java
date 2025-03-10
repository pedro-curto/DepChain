package depchain.common.domain;

import depchain.common.messaging.StateMessage;
import depchain.common.messaging.WriteMessage;

import java.util.ArrayList;
import java.util.List;

public class ConsensusState {

	private String memberName;

	private ValueTimestampPair current;

	private List<ValueTimestampPair> writeset;

	private int currentConsensusInstance;

	private final Object lock = new Object();

	private List<WriteMessage> writeMessages;

	public ConsensusState(String memberName, int currentConsensusInstance) {
		// Initial State
		this.memberName = memberName;
		this.current = null;
		this.writeset = new ArrayList<>();
		this.currentConsensusInstance = currentConsensusInstance;
		this.writeMessages = new ArrayList<>();
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

	public void setCurrent(ValueTimestampPair current) {
		this.current = current;
	}

	public void addWritesetEntry(ValueTimestampPair entry) {
		this.writeset.add(entry);
	}

	public boolean isInitialState() {
		return this.current == null;
	}

	public void addWriteMessage(WriteMessage write) {
		synchronized (lock) {
			writeMessages.add(write);
			lock.notifyAll();
		}
	}

	@Override
	public String toString() {
		return "ConsensusState{" +
				"memberName='" + memberName + '\'' +
				", current=" + current +
				", writeset=" + writeset +
				'}';
	}

	public List<WriteMessage> waitForWriteQuorum(int byzantineQuorum) {
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
		return writeMessages;
	}
}
