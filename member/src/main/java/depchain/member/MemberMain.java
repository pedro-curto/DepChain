package depchain.member;

import java.util.List;

import depchain.member.membership.Member;
import depchain.member.membership.MemberData;
import depchain.member.membership.MembershipManager;

public class MemberMain {
//    private static int port;
//    private static String address;
//    private static String memberName;
    private static final String MEMBERSHIP_FILE = "membership/membership.txt";

    public static void main (String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<port> <address> <memberName>\"");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String address = args[1];
        String memberName = args[2];
        System.out.println("Member " + memberName + " started at port " + port);

        // load membership from file
        List<MemberData> membershipInfo = MembershipManager.loadMembership(MEMBERSHIP_FILE);
        System.out.println("Membership: " + membershipInfo);

        // create my member object and run start (creates other relevant structures and listener)
        Member myself = new Member(memberName, membershipInfo, port, address);
        myself.start();
//        System.out.println("Am I leader? " + myself.isLeader());
//        DatagramSocket serverSocket = new DatagramSocket(port);
//        BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
//
//        // generates symmetric keys for all processes with ports bigger than mine
//        myself.generateMembersSecretKeys();
//
//        // initializing blockchain
//        RequestHandler requestHandler = new RequestHandler(new BlockchainState(new ArrayList<>()));
//
//        // starts message handler and perfect link
//        PerfectLink perfectLink = new PerfectLink(serverSocket, messageQueue);
//        perfectLink.start();
//
//        while (true) {
//            Message message = messageQueue.take();
//        }
    }

}
