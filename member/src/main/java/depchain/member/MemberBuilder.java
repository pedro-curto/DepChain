package depchain.member;

import depchain.common.DCLogger;
import depchain.common.PerfectLink;
import depchain.common.PerfectLinkByzantine;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.messaging.AppendMessage;
import depchain.common.messaging.Message;
import depchain.member.byzantine.*;
import depchain.member.domain.Config;
import depchain.member.domain.ConsensusLeaderState;
import depchain.member.domain.Member;
import depchain.member.state.BlockchainState;

import java.net.DatagramSocket;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class MemberBuilder {

    public static Member build(String name, String address, int port, int behaviour) throws Exception {
        Config config = new Config(name, address, port);
        DCLogger logger = new DCLogger(Member.class, true, config.getBaseDir() + "/logs/member-" + config.getMyName() + ".log");
        BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        BlockingQueue<AppendMessage> appendQueue = new LinkedBlockingQueue<>();

        // create perfect link
        DatagramSocket serverSocket = new DatagramSocket(config.getPort());
        List<Entity> entities = new ArrayList<>();
        entities.addAll(config.getClients());
        entities.addAll(config.getMembers());
        KeyPair myKeyPair = config.getMyKeyPair();
        PerfectLink pf = new PerfectLink(serverSocket, messageQueue, myKeyPair, entities, false);

        // states
        BlockchainState blockchainState = new BlockchainState(new ArrayList<>());
        ConsensusState consensusState =
                config.getLeader().getEntityName().equalsIgnoreCase(config.getMyName()) ?
                        new ConsensusLeaderState(config.getMyName(), 0) :
                        new ConsensusState(config.getMyName(), 0);

        return switch (behaviour) {
            case 1 ->
                    new NoAnswerByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case 2 ->
                    new CoordinatedWrongStateByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case 3 ->
                    new FakeSignatureByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case 4 ->
                    new ReplaySignatureByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case 5 ->
                    new SpamByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case 6 ->
                    new WrongWriteAcceptByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case 7 ->
                    new MemberPerfectLinkByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            default ->
                    new Member(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
        };
    }


    /**
     * Builder for the tests
     */
    public static Member testBuild(
            String name,
            List<Entity> members,
            List<Entity> clients,
            int port,
            String address,
            String byzantineType
    ) throws Exception {

        Config config = new Config(name, address, port);
        config.setMembers(members);
        config.setClients(clients);
        DCLogger logger = new DCLogger(Member.class, true, config.getBaseDir() + "/logs/test/member-" + config.getMyName() + ".log");
        BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        BlockingQueue<AppendMessage> appendQueue = new LinkedBlockingQueue<>();

        // create perfect link
        DatagramSocket serverSocket = new DatagramSocket(config.getPort());
        List<Entity> entities = new ArrayList<>();
        entities.addAll(config.getClients());
        entities.addAll(config.getMembers());
        KeyPair myKeyPair = config.getMyKeyPair();
        PerfectLink pf = byzantineType.equals("byz-perfect-link") ?
                new PerfectLinkByzantine(serverSocket, messageQueue, myKeyPair, entities, true) :
                new PerfectLink(serverSocket, messageQueue, myKeyPair, entities, false);

        // states
        BlockchainState blockchainState = new BlockchainState(new ArrayList<>());
        ConsensusState consensusState =
                config.getLeader().getEntityName().equalsIgnoreCase(config.getMyName()) ?
                        new ConsensusLeaderState(config.getMyName(), 0) :
                        new ConsensusState(config.getMyName(), 0);

        return switch (byzantineType) {
            case "no-answer" ->
                    new NoAnswerByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case "fake-signature"->
                    new FakeSignatureByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case "spam"->
                    new SpamByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case "wrong-state"->
                    new CoordinatedWrongStateByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case "replay-signature"->
                    new ReplaySignatureByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case "wrong-write-accept"->
                    new WrongWriteAcceptByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            case "byz-perfect-link"->
                    new MemberPerfectLinkByzantine(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
            default ->
                    new Member(config, logger, pf, consensusState, blockchainState, messageQueue, appendQueue);
        };
    }

}
