package depchain.member;

import java.util.List;

import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.member.byzantine.NoAnswerByzantine;
import depchain.member.domain.Member;

public class MemberMain {
    private static final String MEMBERSHIP_FILE = "membership/membership.txt";
    private static final String CLIENT_FILE = "membership/client.txt";

    public static void main (String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<port> <address> <memberName> [byzantineBehaviour]\"");
            System.out.println("byzantineBehaviour: 0 - no Byzantine behaviour, 1 - ignore messages");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String address = args[1];
        String memberName = args[2];
        int byzantineBehaviour = args.length == 4 ? Integer.parseInt(args[3]) : 0;
        System.out.println("Member " + memberName + " started at port " + port + " with byzantine behaviour " + byzantineBehaviour);

        // load membership from file
        List<Entity> membershipInfo = CommonUtils.loadMembership(MEMBERSHIP_FILE);
        List<Entity> clients = CommonUtils.loadMembership(CLIENT_FILE);
        System.out.println("Membership: " + membershipInfo);

        // create my member object and run start (creates other relevant structures and listener)
        boolean debug = true;
        Member myself;
        switch (byzantineBehaviour) {
            case 0:
                myself = new Member(memberName, membershipInfo, clients, port, address, debug);
                myself.start();
                break;
            case 1:
                myself = new NoAnswerByzantine(memberName, membershipInfo, clients, port, address, debug);
                myself.start();
                break;
            default:
                System.out.println("Invalid byzantine behaviour");
                System.exit(1);
        }
    }

}
