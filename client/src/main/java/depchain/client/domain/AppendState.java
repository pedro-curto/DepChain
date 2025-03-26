package depchain.client.domain;

import java.util.HashMap;
import java.util.Map;

// goal is to have the response counters and if a decision was already made regarding this string
public class AppendState {
    private final int faultyProcesses;
    private final int byzantineQuorum;
    private final Map<String, Integer> equalAnswersCounter;
    private int totalAnswersCounter;
    private boolean appended;

    public AppendState(int faultyProcesses, int byzantineQuorum) {
        this.faultyProcesses = faultyProcesses;
        this.byzantineQuorum = byzantineQuorum;
        this.equalAnswersCounter = new HashMap<>();
        this.totalAnswersCounter = 0;
        this.appended = false;
    }

    public int getFaultyProcesses() {
        return faultyProcesses;
    }

    public int getByzantineQuorum() {
        return byzantineQuorum;
    }

    public Map<String, Integer> getEqualAnswersCounter() {
        return equalAnswersCounter;
    }

    public int getTotalAnswersCounter() {
        return totalAnswersCounter;
    }

    public void setTotalAnswersCounter(int totalAnswersCounter) {
        this.totalAnswersCounter = totalAnswersCounter;
    }

    public boolean getAppended() {
        return appended;
    }

    public void setAppended(boolean appended) {
        this.appended = appended;
    }
}
