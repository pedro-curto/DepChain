package depchain.member;

import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import depchain.member.messaging.MessageHandlerImpl;
import depchain.member.links.PerfectLink;
import depchain.member.membership.Member;
import depchain.member.membership.MemberData;
import depchain.member.membership.MembershipManager;

public class MemberMain {
    private static int port;
    private static String memberName;
    private static final String MEMBERSHIP_FILE = "membership/membership.txt";

    public static void main (String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<port> <memberName>\"");
            System.exit(1);
        }
        port = Integer.parseInt(args[0]);
        memberName = args[1];
        System.out.println("Member " + memberName + " started at port " + port);

        // load membership from file
        List<MemberData> membershipInfo = MembershipManager.loadMembership(MEMBERSHIP_FILE);
        System.out.println("Membership: " + membershipInfo);

        // UDP socket, msg queue and member object
        Member myself = new Member(memberName, membershipInfo);
        System.out.println("Am I leader? " + myself.isLeader());
        DatagramSocket serverSocket = new DatagramSocket(port);
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        // starts message handler and perfect link
        PerfectLink perfectLink = new PerfectLink(serverSocket, messageQueue, myself);
        perfectLink.setMessageHandler(new MessageHandlerImpl(myself, perfectLink));
        perfectLink.start();

        while (true) {
            String message = messageQueue.take();
            System.out.println("[MESSAGE]: " + message);
        }
    }

}
