package depchain.member.domain;

import depchain.common.CommonUtils;
import depchain.common.Security;
import depchain.common.domain.Entity;

import java.security.KeyPair;
import java.util.List;

public class Config {
    private String baseDir = System.getProperty("user.dir");
    private static final String LEADER_FILE = "/membership/leader.txt";
    private static final String MEMBERSHIP_FILE = "membership/membership.txt";
    private static final String CLIENT_FILE = "membership/client.txt";
    protected Entity leader;
    protected List<Entity> members;
    private List<Entity> clients;
    protected final String myName;
    private final String address;
    protected final int port;
    private final int faultyProcesses;
    protected final int byzantineQuorum;
    protected final KeyPair myKeyPair;

    public Config(String myName, String address, int port) {
        this.myName = myName;
        this.address = address;
        this.port = port;
        this.members = CommonUtils.loadMembership(MEMBERSHIP_FILE);
        this.clients = CommonUtils.loadMembership(CLIENT_FILE);
        this.leader = CommonUtils.getLeader(baseDir + LEADER_FILE);
        this.faultyProcesses = Math.floorDiv(members.size() - 1, 3);
        this.byzantineQuorum = members.size() - faultyProcesses;
        this.myKeyPair = Security.getMemberKeyPair(baseDir, myName);
    }

    public String getBaseDir() {
        return baseDir;
    }
    public static String getLeaderFile() {
        return LEADER_FILE;
    }
    public List<Entity> getMembers() {
        return members;
    }
    public List<Entity> getClients() {
        return clients;
    }
    public Entity getLeader() {
        return leader;
    }
    public String getMyName() {
        return myName;
    }
    public String getAddress() {
        return address;
    }
    public int getPort() {
        return port;
    }
    public int getFaultyProcesses() {
        return faultyProcesses;
    }
    public int getByzantineQuorum() {
        return byzantineQuorum;
    }
    public KeyPair getMyKeyPair() {
        return myKeyPair;
    }
    public void setMembers(List<Entity> members) {
        this.members = members;
    }
    public void setClients(List<Entity> clients) {
        this.clients = clients;
    }
}
