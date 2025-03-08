package depchain.member;

import java.util.List;

import depchain.member.membership.Member;
import depchain.member.membership.MemberData;
import depchain.member.membership.MembershipManager;

public class MemberMain {
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
    }

}
